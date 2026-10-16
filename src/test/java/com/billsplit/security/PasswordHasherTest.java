package com.billsplit.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void hashesAndVerifiesPassword() {
        String hash = hasher.hash("correct-horse");
        assertTrue(hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
        assertTrue(hasher.verify("correct-horse", hash));
        assertFalse(hasher.verify("wrong-password", hash));
    }

    @Test
    void rejectsBlankPassword() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(" "));
    }

    @Test
    void verifyHandlesInvalidHash() {
        assertFalse(hasher.verify("secret", "not-a-bcrypt-hash"));
        assertFalse(hasher.verify("secret", null));
    }
}
