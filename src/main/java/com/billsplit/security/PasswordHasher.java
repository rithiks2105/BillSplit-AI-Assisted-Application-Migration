package com.billsplit.security;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordHasher {
    private static final int ROUNDS = 12;

    public String hash(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(ROUNDS));
    }

    public boolean verify(String plaintext, String hashed) {
        if (plaintext == null || hashed == null || hashed.isBlank()) {
            return false;
        }
        try {
            return BCrypt.checkpw(plaintext, hashed);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
