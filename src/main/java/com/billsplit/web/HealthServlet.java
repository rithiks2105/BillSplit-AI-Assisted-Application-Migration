package com.billsplit.web;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

@WebServlet("/api/health")
public final class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppContext app = AppContext.from(getServletContext());
        if (app == null) {
            JsonSupport.error(resp, 500, "Application is not initialized.");
            return;
        }
        JSONObject body = new JSONObject()
                .put("status", "ok")
                .put("databaseConfigured", app.getDataSourceFactory().isConfigured())
                .put("geminiConfigured", app.getConfig().isGeminiConfigured());
        JsonSupport.write(resp, 200, body);
    }
}
