package com.billsplit.db;

import com.billsplit.service.AppException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DatabaseExceptionsTest {
    @Test
    void mapsMissingConfigurationWithoutLeakingSql() {
        AppException ex = DatabaseExceptions.unavailable(
                new SQLException("Database is not configured. Set BILLSPLIT_DB_URL."));
        assertEquals(503, ex.getStatus());
        assertEquals(
                "Database is not configured. Set BILLSPLIT_DB_URL, BILLSPLIT_DB_USER, and BILLSPLIT_DB_PASSWORD.",
                ex.getMessage());
    }

    @Test
    void hidesOracleDriverMessages() {
        AppException ex = DatabaseExceptions.unavailable(new SQLException("ORA-12541: TNS:no listener"));
        assertEquals(503, ex.getStatus());
        assertEquals("Database is unavailable.", ex.getMessage());
        assertFalse(ex.getMessage().contains("ORA-"));
    }
}
