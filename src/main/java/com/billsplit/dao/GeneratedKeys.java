package com.billsplit.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Reads Oracle identity values from {@code getGeneratedKeys()}.
 * The driver may expose the column as a NUMBER, BigDecimal, or string.
 */
final class GeneratedKeys {
    private GeneratedKeys() {}

    static long read(ResultSet keys, String entity) throws SQLException {
        if (keys == null || !keys.next()) {
            throw new SQLException("Failed to retrieve generated " + entity + " id");
        }
        Object value = keys.getObject(1);
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value == null ? keys.getString(1) : value.toString();
        if (text != null && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                throw new SQLException("Failed to retrieve generated " + entity + " id");
            }
        }
        throw new SQLException("Failed to retrieve generated " + entity + " id");
    }
}
