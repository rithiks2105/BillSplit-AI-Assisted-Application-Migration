package com.billsplit.dao;

import com.billsplit.model.Trip;
import com.billsplit.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TripDao {
    public List<Trip> findByUserId(Connection conn, long userId) throws SQLException {
        String sql = """
                SELECT DISTINCT t.id, t.name, t.start_date
                FROM trips t
                JOIN trip_users tu ON t.id = tu.trip_id
                WHERE tu.user_id = ?
                ORDER BY t.start_date DESC NULLS LAST
                """;
        List<Trip> trips = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    trips.add(mapTrip(rs));
                }
            }
        }
        return trips;
    }

    public boolean exists(Connection conn, long tripId) throws SQLException {
        String sql = "SELECT 1 FROM trips WHERE id = ? FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean isMember(Connection conn, long tripId, long userId) throws SQLException {
        String sql = "SELECT 1 FROM trip_users WHERE trip_id = ? AND user_id = ? FETCH FIRST 1 ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public long insertTrip(Connection conn, String name, LocalDateTime startDate) throws SQLException {
        String sql = "INSERT INTO trips (name, start_date) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[] {"ID"})) {
            ps.setString(1, name);
            ps.setTimestamp(2, Timestamp.valueOf(startDate));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return GeneratedKeys.read(keys, "trip");
            }
        }
    }

    public void addMember(Connection conn, long tripId, long userId) throws SQLException {
        String sql = "INSERT INTO trip_users (trip_id, user_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findMembers(Connection conn, long tripId) throws SQLException {
        String sql = """
                SELECT u.id, u.name, u.email
                FROM users u
                JOIN trip_users tu ON u.id = tu.user_id
                WHERE tu.trip_id = ?
                ORDER BY u.name
                """;
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(new User(rs.getLong("id"), rs.getString("name"), rs.getString("email")));
                }
            }
        }
        return users;
    }

    private static Trip mapTrip(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("start_date");
        return new Trip(rs.getLong("id"), rs.getString("name"), ts == null ? null : ts.toLocalDateTime());
    }
}
