package com.ghostteam.findetect;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class Secrets {
    private final SecureRandom random = new SecureRandom();
    String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

    String hashPassword(String password) {
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

    boolean verifyPassword(
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

    String digest(String token) {
        try {
            byte[] hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
