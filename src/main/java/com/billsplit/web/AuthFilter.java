package com.billsplit.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Set;

@WebFilter("/api/*")
public final class AuthFilter extends HttpFilter {
    private static final Set<String> PUBLIC = Set.of(
            "/api/login",
            "/api/signup",
            "/api/logout",
            "/api/health");

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = request.getServletPath();
        if (request.getPathInfo() != null) {
            path = path + request.getPathInfo();
        }
        if (PUBLIC.contains(path) || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }
        if (Sessions.userIdOrNull(request) == null) {
            JsonSupport.error(response, 401, "Unauthorized: No session.");
            return;
        }
        chain.doFilter(request, response);
    }
}
