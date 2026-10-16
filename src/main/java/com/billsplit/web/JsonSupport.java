package com.billsplit.web;

import com.billsplit.service.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public final class JsonSupport {
    private JsonSupport() {}

    public static JSONObject read(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                body.append(line);
            }
        }
        if (body.isEmpty()) {
            return new JSONObject();
        }
        try {
            return new JSONObject(body.toString());
        } catch (JSONException e) {
            throw new AppException(400, "Invalid JSON.");
        }
    }

    public static BigDecimal decimal(JSONObject body, String key, BigDecimal fallback) {
        if (body == null || !body.has(key) || body.isNull(key)) {
            return fallback;
        }
        try {
            Object value = body.get(key);
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString());
            }
            if (value instanceof String text && !text.isBlank()) {
                return new BigDecimal(text.trim());
            }
            return fallback;
        } catch (NumberFormatException | JSONException e) {
            throw new AppException(400, "Invalid numeric value for " + key + ".");
        }
    }

    public static long requiredLong(JSONObject body, String key) {
        if (body == null || !body.has(key) || body.isNull(key)) {
            throw new AppException(400, "All transaction details are required.");
        }
        try {
            return body.getLong(key);
        } catch (JSONException e) {
            throw new AppException(400, "All transaction details are required.");
        }
    }

    public static void write(HttpServletResponse response, int status, Object body) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(body.toString());
    }

    public static void error(HttpServletResponse response, int status, String message) throws IOException {
        write(response, status, new JSONObject().put("error", message).put("message", message));
    }

    public static void handle(HttpServletResponse response, RuntimeException e) throws IOException {
        if (e instanceof AppException app) {
            error(response, app.getStatus(), app.getMessage());
            return;
        }
        e.printStackTrace();
        error(response, 500, "Internal server error.");
    }
}
