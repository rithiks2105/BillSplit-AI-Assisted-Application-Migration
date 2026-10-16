package com.billsplit.db;

import com.billsplit.service.AppException;

import java.sql.SQLException;

public final class DatabaseExceptions {
    private DatabaseExceptions() {}

    public static AppException unavailable(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("not configured")) {
            return new AppException(
                    503,
                    "Database is not configured. Set BILLSPLIT_DB_URL, BILLSPLIT_DB_USER, and BILLSPLIT_DB_PASSWORD.");
        }
        return new AppException(503, "Database is unavailable.");
    }
}
