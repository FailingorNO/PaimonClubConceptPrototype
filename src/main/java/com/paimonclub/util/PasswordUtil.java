package com.paimonclub.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码工具：BCrypt 加盐散列。
 */
public final class PasswordUtil {

    private static final int LOG_ROUNDS = 10;

    private PasswordUtil() {
    }

    public static String hash(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(LOG_ROUNDS));
    }

    public static boolean matches(String rawPassword, String hash) {
        if (rawPassword == null || hash == null || hash.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
