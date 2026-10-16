package com.billsplit.web;

import com.billsplit.model.User;
import com.billsplit.service.AppException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.JSONObject;

import java.io.IOException;

@WebServlet(urlPatterns = {"/api/login", "/api/signup", "/api/logout", "/api/me"})
public final class AuthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            if ("/api/me".equals(req.getServletPath())) {
                long userId = Sessions.requireUserId(req);
                User user = AppContext.from(getServletContext()).getAuthService().requireUser(userId);
                JsonSupport.write(resp, 200, toUser(user).put("message", "ok"));
                return;
            }
            JsonSupport.error(resp, 405, "Method not allowed.");
        } catch (RuntimeException e) {
            JsonSupport.handle(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String path = req.getServletPath();
            switch (path) {
                case "/api/signup" -> signup(req, resp);
                case "/api/login" -> login(req, resp);
                case "/api/logout" -> {
                    Sessions.logout(req);
                    JsonSupport.write(resp, 200, new JSONObject().put("message", "Logged out"));
                }
                default -> JsonSupport.error(resp, 405, "Method not allowed.");
            }
        } catch (RuntimeException e) {
            JsonSupport.handle(resp, e);
        }
    }

    private void signup(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject body = JsonSupport.read(req);
        User user = AppContext.from(getServletContext()).getAuthService()
                .signup(body.optString("username", null), body.optString("email", null), body.optString("password", null));
        Sessions.login(req, user);
        JsonSupport.write(resp, 200, toUser(user).put("message", "User registered successfully"));
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        JSONObject body = JsonSupport.read(req);
        String username = body.has("username") && !body.optString("username").isBlank()
                ? body.optString("username")
                : body.optString("name", null);
        User user = AppContext.from(getServletContext()).getAuthService()
                .login(username, body.optString("password", null));
        Sessions.login(req, user);
        JsonSupport.write(resp, 200, toUser(user).put("message", "Login successful"));
    }

    private static JSONObject toUser(User user) {
        return new JSONObject()
                .put("userId", user.getId())
                .put("id", user.getId())
                .put("username", user.getName())
                .put("name", user.getName())
                .put("email", user.getEmail());
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException, jakarta.servlet.ServletException {
        try {
            super.service(req, resp);
        } catch (AppException e) {
            JsonSupport.error(resp, e.getStatus(), e.getMessage());
        }
    }
}
