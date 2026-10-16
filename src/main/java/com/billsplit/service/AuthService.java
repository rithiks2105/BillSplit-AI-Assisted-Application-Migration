package com.billsplit.service;

import com.billsplit.dao.UserDao;
import com.billsplit.db.DatabaseExceptions;
import com.billsplit.db.DataSourceFactory;
import com.billsplit.model.User;
import com.billsplit.security.PasswordHasher;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

public final class AuthService {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private final DataSourceFactory dataSourceFactory;
    private final UserDao userDao;
    private final PasswordHasher passwordHasher;

    public AuthService(DataSourceFactory dataSourceFactory, UserDao userDao, PasswordHasher passwordHasher) {
        this.dataSourceFactory = dataSourceFactory;
        this.userDao = userDao;
        this.passwordHasher = passwordHasher;
    }

    public User signup(String username, String email, String password) {
        String name = requireUsername(username);
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new AppException(400, "A valid email is required.");
        }
        if (email.trim().length() > 255) {
            throw new AppException(400, "Email is too long.");
        }
        if (password == null || password.length() < 8) {
            throw new AppException(400, "Password must be at least 8 characters.");
        }
        if (password.length() > 72) {
            throw new AppException(400, "Password must be at most 72 characters.");
        }
        try (Connection conn = dataSourceFactory.getConnection()) {
            long id = userDao.insert(conn, name, email.trim(), passwordHasher.hash(password));
            return new User(id, name, email.trim());
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new AppException(409, "Username or email is already registered.");
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                throw new AppException(409, "Username or email is already registered.");
            }
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public User login(String username, String password) {
        String name = requireUsername(username);
        if (password == null || password.isBlank()) {
            throw new AppException(400, "Password is required.");
        }
        try (Connection conn = dataSourceFactory.getConnection()) {
            User user = userDao.findByName(conn, name)
                    .orElseThrow(() -> new AppException(401, "Invalid username."));
            String hash = userDao.findPasswordHashByName(conn, name).orElse("");
            if (!passwordHasher.verify(password, hash)) {
                throw new AppException(401, "Invalid password.");
            }
            return user;
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public User requireUser(long userId) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            return userDao.findById(conn, userId)
                    .orElseThrow(() -> new AppException(401, "Session is no longer valid."));
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    private static String requireUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AppException(400, "Username is required.");
        }
        String name = username.trim();
        if (name.length() > 255) {
            throw new AppException(400, "Username is too long.");
        }
        return name;
    }

    private static boolean isUniqueViolation(SQLException e) {
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return e.getErrorCode() == 1
                || "23000".equals(e.getSQLState())
                || message.contains("unique")
                || message.contains("ora-00001");
    }

}
