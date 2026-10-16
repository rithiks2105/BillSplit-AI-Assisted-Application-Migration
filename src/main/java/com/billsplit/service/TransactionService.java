package com.billsplit.service;

import com.billsplit.dao.TransactionDao;
import com.billsplit.dao.TripDao;
import com.billsplit.db.DatabaseExceptions;
import com.billsplit.db.DataSourceFactory;
import com.billsplit.model.Expense;
import com.billsplit.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class TransactionService {
    private final DataSourceFactory dataSourceFactory;
    private final TripDao tripDao;
    private final TransactionDao transactionDao;
    private final ExpenseValidator validator;

    public TransactionService(
            DataSourceFactory dataSourceFactory,
            TripDao tripDao,
            TransactionDao transactionDao,
            ExpenseValidator validator) {
        this.dataSourceFactory = dataSourceFactory;
        this.tripDao = tripDao;
        this.transactionDao = transactionDao;
        this.validator = validator;
    }

    public List<Expense> list(long userId, long tripId) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            requireMember(conn, tripId, userId);
            return transactionDao.findByTripId(conn, tripId);
        } catch (SQLException e) {
            throw DatabaseExceptions.unavailable(e);
        }
    }

    public long create(long userId, long tripId, Expense expense) {
        try (Connection conn = dataSourceFactory.getConnection()) {
            requireMember(conn, tripId, userId);
            Set<Long> memberIds = tripDao.findMembers(conn, tripId).stream()
                    .map(User::getId)
                    .collect(Collectors.toSet());
            validator.validate(expense, memberIds);
            conn.setAutoCommit(false);
            try {
                long transactionId = transactionDao.insertTransaction(conn, tripId, expense);
                transactionDao.insertShares(conn, transactionId, expense.getUsers());
                conn.commit();
                return transactionId;
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

    private void requireMember(Connection conn, long tripId, long userId) throws SQLException {
        if (!tripDao.exists(conn, tripId)) {
            throw new AppException(404, "Trip not found.");
        }
        if (!tripDao.isMember(conn, tripId, userId)) {
            throw new AppException(403, "Unauthorized: You do not have access to this trip's transactions.");
        }
    }

}
