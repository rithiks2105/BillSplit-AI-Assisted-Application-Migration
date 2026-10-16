package com.billsplit.service;

import com.billsplit.config.AppConfig;
import com.billsplit.model.Expense;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void reportsUnconfiguredWhenNoGeminiKey() throws Exception {
        Path file = tempDir.resolve("empty.properties");
        Files.writeString(file, "db.url=\n");
        if (System.getenv(AppConfig.ENV_GEMINI_KEY) != null) {
            return;
        }
        SummaryService service = new SummaryService(new AppConfig(file.toString()));
        SummaryService.SummaryResult result = service.summarize(List.<Expense>of());
        assertFalse(result.configured());
        assertTrue(result.text().contains("not configured"));
    }
}
