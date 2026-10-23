package com.smartcart.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/** Password hashing: SHA-256 with a random per-user salt, stored as "salt:hash". */
public final class Security {
    private Security() {}

    private static String sha(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : d) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String hash(String password) {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);
        StringBuilder sb = new StringBuilder();
        for (byte x : salt) sb.append(String.format("%02x", x));
        String s = sb.toString();
        return s + ":" + sha(s + password);
    }

    public static boolean verify(String password, String stored) {
        String[] p = stored.split(":");
        return p.length == 2 && sha(p[0] + password).equals(p[1]);
    }
}
