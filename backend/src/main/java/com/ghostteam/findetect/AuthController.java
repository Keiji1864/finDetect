package com.ghostteam.findetect;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JdbcTemplate db;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();

    public AuthController(
            JdbcTemplate db,
            PlatformTransactionManager transactionManager
    ) {
        this.db = db;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public record Credentials(
            String name,
            String login,
            String password
    ) {}

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private String normalizeLogin(String value) {
        if (value == null) {
            throw bad("Введите почту или телефон");
        }

        String login = value.trim().toLowerCase(Locale.ROOT);

        boolean email = login.matches(
                "[^\\s@]+@[^\\s@]+\\.[^\\s@]+"
        );

        boolean phone = login.matches(
                "\\+[1-9][0-9]{7,14}"
        );

        if (login.length() > 254 || (!email && !phone)) {
            throw bad(
                    "Введите почту или телефон в формате +79991234567"
            );
        }

        return login;
    }

    private void validatePassword(String password) {
        if (
                password == null
                        || password.length() < 8
                        || password.length() > 128
        ) {
            throw bad("Пароль должен содержать от 8 до 128 символов");
        }
    }

    private byte[] derive(
            String password,
            byte[] salt
    ) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                600000,
                256
        );

        try {
            return SecretKeyFactory
                    .getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec)
                    .getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            return "pbkdf2$600000$"
                    + Base64.getEncoder().encodeToString(salt)
                    + "$"
                    + Base64.getEncoder().encodeToString(
                    derive(password, salt)
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean verifyPassword(
            String password,
            String stored
    ) {
        try {
            String[] parts = stored.split("\\$");

            if (
                    parts.length != 4
                            || !parts[0].equals("pbkdf2")
                            || !parts[1].equals("600000")
            ) {
                return false;
            }

            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt);

            return MessageDigest.isEqual(actual, expected);
        } catch (Exception e) {
            return false;
        }
    }

    private String digest(String token) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private Map<String, Object> createSession(UUID userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);

        String token = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);

        db.update(
                """
                INSERT INTO sessions (
                    user_id,
                    refresh_token_hash,
                    expires_at
                )
                VALUES (?, ?, now() + interval '1 day')
                """,
                userId,
                digest(token)
        );

        return Map.of(
                "token", token,
                "userId", userId.toString()
        );
    }

    @PostMapping("/register")
    public Map<String, Object> register(
            @RequestBody Credentials credentials
    ) {
        String login = normalizeLogin(credentials.login());
        validatePassword(credentials.password());

        if (
                credentials.name() == null
                        || credentials.name().trim().isEmpty()
                        || credentials.name().trim().length() > 100
        ) {
            throw bad("Введите имя до 100 символов");
        }

        String passwordHash = hashPassword(credentials.password());

        try {
            return tx.execute(status -> {
                UUID userId = UUID.randomUUID();

                db.update(
                        """
                        INSERT INTO users (
                            id,
                            display_name,
                            password_hash,
                            status
                        )
                        VALUES (?, ?, ?, 'active')
                        """,
                        userId,
                        credentials.name().trim(),
                        passwordHash
                );

                db.update(
                        """
                        INSERT INTO user_identities (
                            user_id,
                            type,
                            login
                        )
                        VALUES (?, ?, ?)
                        """,
                        userId,
                        login.contains("@") ? "email" : "phone",
                        login
                );

                db.update(
                        "INSERT INTO user_settings (user_id) VALUES (?)",
                        userId
                );

                return createSession(userId);
            });
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Этот логин уже используется"
            );
        }
    }

    @PostMapping("/login")
    public Map<String, Object> login(
            @RequestBody Credentials credentials
    ) {
        String login = normalizeLogin(credentials.login());
        validatePassword(credentials.password());

        var users = db.queryForList(
                """
                SELECT u.id, u.password_hash
                FROM users u
                JOIN user_identities i ON i.user_id = u.id
                WHERE i.login = ?
                  AND u.status = 'active'
                """,
                login
        );

        if (
                users.size() != 1
                        || !verifyPassword(
                        credentials.password(),
                        users.get(0).get("password_hash").toString()
                )
        ) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Неверный логин или пароль"
            );
        }

        return createSession((UUID) users.get(0).get("id"));
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : "";

        db.update(
                """
                UPDATE sessions
                SET revoked_at = now()
                WHERE refresh_token_hash = ?
                  AND revoked_at IS NULL
                """,
                digest(token)
        );

        return Map.of("success", true);
    }

    @GetMapping("/me")
    public Map<String, Object> me(
            @RequestHeader("Authorization") String authorization
    ) {
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7)
                : "";

        var users = db.queryForList(
                """
                SELECT u.id, u.display_name
                FROM sessions s
                JOIN users u ON u.id = s.user_id
                WHERE s.refresh_token_hash = ?
                  AND s.revoked_at IS NULL
                  AND s.expires_at > now()
                  AND u.status = 'active'
                """,
                digest(token)
        );

        if (users.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Войдите снова"
            );
        }

        return users.get(0);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(
            ResponseStatusException exception
    ) {
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.of(
                        "message",
                        Objects.requireNonNullElse(
                                exception.getReason(),
                                "Ошибка запроса"
                        )
                ));
    }
}