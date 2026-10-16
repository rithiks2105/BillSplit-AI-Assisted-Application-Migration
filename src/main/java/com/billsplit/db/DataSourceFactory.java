package com.billsplit.db;

import com.billsplit.config.AppConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DataSourceFactory {
    private final AppConfig config;
    private volatile boolean driverLoaded;

    public DataSourceFactory(AppConfig config) {
        this.config = config;
    }

    public boolean isConfigured() {
        return config.isDatabaseConfigured();
    }

    public Connection getConnection() throws SQLException {
        if (!isConfigured()) {
            throw new SQLException("Database is not configured. Set BILLSPLIT_DB_URL, BILLSPLIT_DB_USER, and BILLSPLIT_DB_PASSWORD.");
        }
        loadDriver();
        return DriverManager.getConnection(
                config.dbUrl().orElseThrow(),
                config.dbUser().orElseThrow(),
                config.dbPassword().orElseThrow());
    }

    private void loadDriver() throws SQLException {
        if (driverLoaded) {
            return;
        }
        synchronized (this) {
            if (driverLoaded) {
                return;
            }
            try {
                Class.forName("oracle.jdbc.OracleDriver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Oracle JDBC driver not found on the classpath", e);
            }
        }
    }
}
