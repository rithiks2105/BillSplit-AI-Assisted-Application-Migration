package com.billsplit.db;

import com.billsplit.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Database-dependent tests. {@link #connectsWhenOracleIsConfigured()} is skipped
 * unless BILLSPLIT_DB_URL is set because Oracle XE is not assumed to be installed.
 */
class OracleConnectionTest {
    @Test
    void reportsUnconfiguredWhenDatabaseSettingsAreAbsent() {
        assumeTrue(
                System.getenv(AppConfig.ENV_DB_URL) == null
                        && System.getenv(AppConfig.ENV_DB_USER) == null
                        && System.getenv(AppConfig.ENV_DB_PASSWORD) == null,
                "Oracle environment variables are set; skipping the unconfigured assertion.");
        DataSourceFactory factory = new DataSourceFactory(new AppConfig("missing-config.properties"));
        assertFalse(factory.isConfigured());
    }

    @Test
    void getConnectionFailsFastWhenDatabaseSettingsAreAbsent() {
        assumeTrue(
                System.getenv(AppConfig.ENV_DB_URL) == null
                        && System.getenv(AppConfig.ENV_DB_USER) == null
                        && System.getenv(AppConfig.ENV_DB_PASSWORD) == null,
                "Oracle environment variables are set; skipping the unconfigured connection assertion.");
        DataSourceFactory factory = new DataSourceFactory(new AppConfig("missing-config.properties"));
        SQLException ex = assertThrows(SQLException.class, factory::getConnection);
        assertTrue(ex.getMessage().contains("not configured"));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "BILLSPLIT_DB_URL", matches = ".+")
    void connectsWhenOracleIsConfigured() throws Exception {
        DataSourceFactory factory = new DataSourceFactory(new AppConfig());
        assertTrue(factory.isConfigured());
        try (Connection conn = factory.getConnection()) {
            assertTrue(conn.isValid(5));
        }
    }
}
