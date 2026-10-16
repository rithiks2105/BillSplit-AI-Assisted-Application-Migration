package com.billsplit.web;

import com.billsplit.model.User;
import com.billsplit.service.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public final class Sessions {
    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";

    private Sessions() {}

    public static void login(HttpServletRequest request, User user) {
        HttpSession existing = request.getSession(false);
        if (existing != null) {
            existing.invalidate();
        }
        HttpSession session = request.getSession(true);
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(USERNAME, user.getName());
        session.setMaxInactiveInterval(60 * 60 * 8);
    }

    public static void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public static long requireUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(USER_ID) == null) {
            throw new AppException(401, "Unauthorized: No session.");
        }
        return (Long) session.getAttribute(USER_ID);
    }

    public static Long userIdOrNull(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object id = session.getAttribute(USER_ID);
        return id instanceof Long value ? value : null;
    }
}
