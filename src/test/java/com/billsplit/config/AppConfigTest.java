package com.billsplit.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void ignoresPlaceholderCredentialsFromExampleFile() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.writeString(file, """
                db.url=jdbc:oracle:thin:@localhost:1521/XEPDB1
                db.user=YOUR_ORACLE_USERNAME
                db.password=YOUR_ORACLE_PASSWORD
                gemini.api.key=
                """);
        AppConfig config = new AppConfig(file.toString());
        if (System.getenv(AppConfig.ENV_DB_URL) == null) {
            assertFalse(config.isDatabaseConfigured());
        }
        if (System.getenv(AppConfig.ENV_GEMINI_KEY) == null) {
            assertFalse(config.isGeminiConfigured());
        }
        assertEquals(file, config.loadedFrom());
    }

    @Test
    void readsNonPlaceholderFileValuesWhenEnvAbsent() throws Exception {
        Path file = tempDir.resolve("application.properties");
        Files.writeString(file, """
                gemini.api.key=test-key-not-for-production
                """);
        AppConfig config = new AppConfig(file.toString());
        if (System.getenv(AppConfig.ENV_GEMINI_KEY) == null) {
            assertTrue(config.isGeminiConfigured());
            assertEquals("test-key-not-for-production", config.geminiApiKey().orElseThrow());
        }
    }
}
