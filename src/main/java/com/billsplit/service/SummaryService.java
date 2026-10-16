package com.billsplit.service;

import com.billsplit.config.AppConfig;
import com.billsplit.model.Expense;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public final class SummaryService {
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";

    private final AppConfig config;
    private final HttpClient httpClient;

    public SummaryService(AppConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
    }

    SummaryService(AppConfig config, HttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    public SummaryResult summarize(List<Expense> expenses) {
        if (!config.isGeminiConfigured()) {
            return SummaryResult.unavailable(
                    "Trip summaries are not configured. Set GEMINI_API_KEY (or gemini.api.key) to enable this feature.");
        }
        JSONArray history = new JSONArray();
        for (Expense expense : expenses) {
            JSONObject item = new JSONObject();
            item.put("id", expense.getId());
            item.put("amount", expense.getAmount());
            item.put("note", expense.getNote());
            item.put("date", expense.getDate() == null ? JSONObject.NULL : expense.getDate().toString());
            JSONArray users = new JSONArray();
            expense.getUsers().forEach(share -> {
                JSONObject u = new JSONObject();
                u.put("id", share.getUserId());
                u.put("name", share.getName());
                u.put("lent", share.getLent());
                u.put("borrow", share.getBorrow());
                users.put(u);
            });
            item.put("users", users);
            history.put(item);
        }

        String prompt = "Here is an expense history from a trip:\n\n" + history
                + "\n\nAnalyze the data and summarize where most of the money was spent. "
                + "Identify key spending categories such as food, travel, accommodation, and activities. "
                + "Provide a structured summary in a readable format with clear category headings within 20 lines.";

        JSONObject body = new JSONObject()
                .put("contents", new JSONArray().put(new JSONObject()
                        .put("parts", new JSONArray().put(new JSONObject().put("text", prompt)))));

        try {
            String key = URLEncoder.encode(config.geminiApiKey().orElseThrow(), StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_URL + "?key=" + key))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return SummaryResult.unavailable("Failed to generate summary.");
            }
            JSONObject json = new JSONObject(response.body());
            String text = json.optJSONArray("candidates") == null
                    ? null
                    : json.getJSONArray("candidates")
                            .optJSONObject(0)
                            .optJSONObject("content")
                            .optJSONArray("parts")
                            .optJSONObject(0)
                            .optString("text", null);
            if (text == null || text.isBlank()) {
                return SummaryResult.unavailable("No summary available.");
            }
            return SummaryResult.ok(text);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return SummaryResult.unavailable("Failed to generate summary.");
        } catch (IOException | RuntimeException e) {
            return SummaryResult.unavailable("Failed to generate summary.");
        }
    }

    public record SummaryResult(boolean configured, String text) {
        static SummaryResult ok(String text) {
            return new SummaryResult(true, text);
        }

        static SummaryResult unavailable(String message) {
            return new SummaryResult(false, message);
        }
    }
}
