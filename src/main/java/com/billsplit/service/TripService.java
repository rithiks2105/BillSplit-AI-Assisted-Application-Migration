package com.billsplit.service;

import com.billsplit.dao.TripDao;
import com.billsplit.dao.UserDao;
import com.billsplit.db.DatabaseExceptions;
import com.billsplit.db.DataSourceFactory;
import com.billsplit.model.Trip;
import com.billsplit.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public final class TripService {
    private final DataSourceFactory dataSourceFactory;
    private final TripDao tripDao;
    private final UserDao userDao;

    public TripService(DataSourceFactory dataSourceFactory, TripDao tripDao, UserDao userDao) {
        this.dataSourceFactory = dataSourceFactory;
        this.tripDao = tripDao;
        this.userDao = userDao;
    }

    public List<Trip> listTrips(long userId) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            return tripDao.findByUserId(conn, userId);
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public Trip createTrip(long userId, String name) {
        if (name == null || name.isBlank()) {
            throw new AppException(400, "Trip name and user ID are required.");
        }
        if (name.trim().length() > 255) {
            throw new AppException(400, "Trip name is too long.");
        }
        try (Connection conn = dataSourceFactory.getConnection()) {
            conn.setAutoCommit(false);
            try {
                LocalDateTime start = LocalDateTime.now();
                long tripId = tripDao.insertTrip(conn, name.trim(), start);
                tripDao.addMember(conn, tripId, userId);
                conn.commit();
                return new Trip(tripId, name.trim(), start);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public List<User> listMembers(long userId, long tripId) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            requireMember(conn, tripId, userId);
            return tripDao.findMembers(conn, tripId);
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public User addMember(long requesterId, long tripId, String username) {
        if (username == null || username.isBlank()) {
            throw new AppException(400, "Username is required.");
        }
        try (Connection conn = dataSourceFactory.getConnection()) {
            requireMember(conn, tripId, requesterId);
            User user = userDao.findByName(conn, username.trim())
                    .orElseThrow(() -> new AppException(404, "User not found."));
            if (tripDao.isMember(conn, tripId, user.getId())) {
                throw new AppException(400, "User is already in the trip.");
            }
            tripDao.addMember(conn, tripId, user.getId());
            return user;
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public void requireMember(long userId, long tripId) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            requireMember(conn, tripId, userId);
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    private void requireMember(Connection conn, long tripId, long userId) throws SQLException {
        if (!tripDao.exists(conn, tripId)) {
            throw new AppException(404, "Trip not found.");
        }
        if (!tripDao.isMember(conn, tripId, userId)) {
            throw new AppException(403, "Unauthorized: You do not have access to this trip.");
        }
    }

}
