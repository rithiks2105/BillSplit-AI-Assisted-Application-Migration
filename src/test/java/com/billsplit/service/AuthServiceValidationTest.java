package com.billsplit.service;

import com.billsplit.config.AppConfig;
import com.billsplit.dao.UserDao;
import com.billsplit.db.DataSourceFactory;
import com.billsplit.security.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceValidationTest {
    private final AuthService authService = new AuthService(
            new DataSourceFactory(new AppConfig("missing-config.properties")),
            new UserDao(),
            new PasswordHasher());

    @Test
    void signupRejectsInvalidEmailWithoutDatabase() {
        AppException ex = assertThrows(AppException.class,
                () -> authService.signup("alice", "not-an-email", "password1"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void signupRejectsShortPasswordWithoutDatabase() {
        AppException ex = assertThrows(AppException.class,
                () -> authService.signup("alice", "alice@example.com", "short"));
        assertEquals(400, ex.getStatus());
        assertEquals("Password must be at least 8 characters.", ex.getMessage());
    }

    @Test
    void loginRejectsBlankUsernameWithoutDatabase() {
        AppException ex = assertThrows(AppException.class, () -> authService.login("  ", "password1"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void signupWithoutDatabaseReturns503AfterValidation() {
        AppException ex = assertThrows(AppException.class,
                () -> authService.signup("alice", "alice@example.com", "password1"));
        assertEquals(503, ex.getStatus());
        assertEquals(
                "Database is not configured. Set BILLSPLIT_DB_URL, BILLSPLIT_DB_USER, and BILLSPLIT_DB_PASSWORD.",
                ex.getMessage());
    }
}
