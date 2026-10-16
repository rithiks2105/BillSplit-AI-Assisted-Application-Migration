package com.billsplit.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Loads configuration from environment variables first, then an optional
 * external properties file. Never stores default secrets.
 */
public final class AppConfig {
    public static final String ENV_DB_URL = "BILLSPLIT_DB_URL";
    public static final String ENV_DB_USER = "BILLSPLIT_DB_USER";
    public static final String ENV_DB_PASSWORD = "BILLSPLIT_DB_PASSWORD";
    public static final String ENV_GEMINI_KEY = "GEMINI_API_KEY";
    public static final String ENV_CONFIG_PATH = "BILLSPLIT_CONFIG";

    private final Properties fileProperties = new Properties();
    private final Path loadedFrom;

    public AppConfig() {
        this(System.getenv(ENV_CONFIG_PATH));
    }

    public AppConfig(String configPath) {
        Path path = resolveConfigPath(configPath);
        this.loadedFrom = path;
        if (path != null && Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                fileProperties.load(in);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to read config file: " + path, e);
            }
        }
    }

    static Path resolveConfigPath(String configPath) {
        if (configPath != null && !configPath.isBlank()) {
            return Path.of(configPath);
        }
        Path local = Path.of("config", "application.properties");
        if (Files.isRegularFile(local)) {
            return local;
        }
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null) {
            Path underTomcat = Path.of(catalinaBase, "conf", "billsplit.properties");
            if (Files.isRegularFile(underTomcat)) {
                return underTomcat;
            }
        }
        return local;
    }

    public Optional<String> dbUrl() {
        return first(ENV_DB_URL, "db.url");
    }

    public Optional<String> dbUser() {
        return first(ENV_DB_USER, "db.user");
    }

    public Optional<String> dbPassword() {
        return first(ENV_DB_PASSWORD, "db.password");
    }

    public Optional<String> geminiApiKey() {
        return first(ENV_GEMINI_KEY, "gemini.api.key");
    }

    public boolean isDatabaseConfigured() {
        return dbUrl().isPresent() && dbUser().isPresent() && dbPassword().isPresent();
    }

    public boolean isGeminiConfigured() {
        return geminiApiKey().filter(k -> !k.isBlank()).isPresent();
    }

    public Path loadedFrom() {
        return loadedFrom;
    }

    private Optional<String> first(String envName, String propertyKey) {
        String env = System.getenv(envName);
        if (env != null && !env.isBlank()) {
            return Optional.of(env);
        }
        String sys = System.getProperty(envName);
        if (sys != null && !sys.isBlank()) {
            return Optional.of(sys);
        }
        String fromFile = fileProperties.getProperty(propertyKey);
        if (fromFile != null && !fromFile.isBlank() && !fromFile.startsWith("YOUR_")) {
            return Optional.of(fromFile.trim());
        }
        return Optional.empty();
    }
}
