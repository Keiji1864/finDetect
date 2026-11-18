package com.ghostteam.findetect;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Locale;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@Transactional
public class AuthController {
    record Register(@NotBlank @Size(max=50) String name, @NotBlank @Email @Size(max=254) String email,
                    @NotNull @Size(min=8,max=128) String password) {}
    record Login(@NotBlank @Email @Size(max=254) String email, @NotNull @Size(max=128) String password) {}
    record Refresh(@NotBlank @Size(max=128) String refreshToken) {}
    record Rename(@NotBlank @Size(max=50) String name) {}
    record User(UUID id, String email, String name) {}
    record Tokens(String accessToken, String refreshToken, long expiresIn) {}
    record Result(User user, Tokens tokens) {}

    private final JdbcTemplate db;
    private final Secrets secrets = new Secrets();
    private final String dummyHash = secrets.hashPassword(secrets.token());

    public AuthController(JdbcTemplate db) { this.db = db; }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Result register(@Valid @RequestBody Register input) {
        User user = new User(UUID.randomUUID(), email(input.email()), input.name().strip());
        try {
            db.update("INSERT INTO users(id,email,name,password_hash) VALUES(?,?,?,?)",
                    user.id(), user.email(), user.name(), secrets.hashPassword(input.password()));
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return new Result(user, issue(user.id()));
    }

    @PostMapping("/auth/login")
    public Result login(@Valid @RequestBody Login input) {
        var rows = db.queryForList("SELECT id,email,name,password_hash FROM users WHERE email=?", email(input.email()));
        String hash = rows.isEmpty() ? dummyHash : (String) rows.get(0).get("password_hash");
        if (!secrets.verifyPassword(input.password(), hash) || rows.isEmpty()) throw unauthorized();
        var row = rows.get(0);
        User user = new User((UUID) row.get("id"), (String) row.get("email"), (String) row.get("name"));
        return new Result(user, issue(user.id()));
    }

    @PostMapping("/auth/refresh")
    public Tokens refresh(@Valid @RequestBody Refresh input) {
        var ids = db.queryForList("""
                DELETE FROM sessions WHERE refresh_hash=? AND refresh_until>now() RETURNING user_id
                """, UUID.class, secrets.digest(input.refreshToken()));
        if (ids.isEmpty()) throw unauthorized();
        return issue(ids.get(0));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody Refresh input) {
        db.update("DELETE FROM sessions WHERE refresh_hash=?", secrets.digest(input.refreshToken()));
    }

    @GetMapping("/me")
    public User me(@RequestHeader(value="Authorization",required=false) String header) {
        return profile(owner(header));
    }

    @PatchMapping("/me")
    public User rename(@RequestHeader(value="Authorization",required=false) String header, @Valid @RequestBody Rename input) {
        UUID id = owner(header);
        db.update("UPDATE users SET name=? WHERE id=?", input.name().strip(), id);
        return profile(id);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value="Authorization",required=false) String header) {
        db.update("DELETE FROM users WHERE id=?", owner(header));
    }

    UUID owner(String header) {
        if (header == null || !header.startsWith("Bearer ") || header.length()>140) throw unauthorized();
        return db.queryForList("SELECT user_id FROM sessions WHERE access_hash=? AND access_until>now()",
                UUID.class, secrets.digest(header.substring(7))).stream().findFirst().orElseThrow(AuthController::unauthorized);
    }

    private User profile(UUID id) {
        return db.queryForObject("SELECT id,email,name FROM users WHERE id=?", (rs,i) ->
                new User(id,rs.getString("email"),rs.getString("name")),id);
    }

    private Tokens issue(UUID id) {
        Tokens pair = new Tokens(secrets.token(), secrets.token(), 900);
        db.update("DELETE FROM sessions WHERE user_id=? AND refresh_until<=now()", id);
        db.update("""
                INSERT INTO sessions(access_hash,refresh_hash,user_id,access_until,refresh_until)
                VALUES(?,?,?,now()+interval '15 minutes',now()+interval '30 days')
                """,secrets.digest(pair.accessToken()),secrets.digest(pair.refreshToken()),id);
        return pair;
    }

    private static String email(String value) { return value.strip().toLowerCase(Locale.ROOT); }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
}
