package com.billsplit.dao;

import com.billsplit.model.Expense;
import com.billsplit.model.TransactionShare;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TransactionDao {
    public long insertTransaction(Connection conn, long tripId, Expense expense) throws SQLException {
        String sql = "INSERT INTO transactions (trip_id, amount, note, txn_date) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, new String[] {"ID"})) {
            ps.setLong(1, tripId);
            ps.setBigDecimal(2, expense.getAmount());
            ps.setString(3, expense.getNote());
            LocalDate date = expense.getDate() == null ? LocalDate.now() : expense.getDate();
            ps.setTimestamp(4, Timestamp.valueOf(date.atStartOfDay()));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return GeneratedKeys.read(keys, "transaction");
            }
        }
    }

    public void insertShares(Connection conn, long transactionId, List<TransactionShare> shares) throws SQLException {
        String sql = "INSERT INTO transaction_users (transaction_id, user_id, borrow, lent) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (TransactionShare share : shares) {
                ps.setLong(1, transactionId);
                ps.setLong(2, share.getUserId());
                ps.setBigDecimal(3, share.getBorrow());
                ps.setBigDecimal(4, share.getLent());
                ps.addBatch();
            }
            if (!shares.isEmpty()) {
                ps.executeBatch();
            }
        }
    }

    public List<Expense> findByTripId(Connection conn, long tripId) throws SQLException {
        String sql = """
                SELECT t.id, t.amount, t.note, t.txn_date,
                       u.id AS user_id, u.name AS user_name, tu.borrow, tu.lent
                FROM transactions t
                LEFT JOIN transaction_users tu ON t.id = tu.transaction_id
                LEFT JOIN users u ON tu.user_id = u.id
                WHERE t.trip_id = ?
                ORDER BY t.txn_date DESC, t.id DESC, u.name
                """;
        Map<Long, ExpenseBuilder> builders = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, tripId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long id = rs.getLong("id");
                    ExpenseBuilder builder = builders.get(id);
                    if (builder == null) {
                        Timestamp txnDate = rs.getTimestamp("txn_date");
                        builder = new ExpenseBuilder(
                                id,
                                rs.getBigDecimal("amount"),
                                rs.getString("note"),
                                txnDate == null ? null : txnDate.toLocalDateTime().toLocalDate());
                        builders.put(id, builder);
                    }
                    long userId = rs.getLong("user_id");
                    if (!rs.wasNull()) {
                        builder.users.add(new TransactionShare(
                                userId,
                                rs.getString("user_name"),
                                rs.getBigDecimal("borrow"),
                                rs.getBigDecimal("lent")));
                    }
                }
            }
        }
        List<Expense> expenses = new ArrayList<>();
        for (ExpenseBuilder builder : builders.values()) {
            expenses.add(builder.build());
        }
        return expenses;
    }

    private static final class ExpenseBuilder {
        private final long id;
        private final java.math.BigDecimal amount;
        private final String note;
        private final LocalDate date;
        private final List<TransactionShare> users = new ArrayList<>();

        private ExpenseBuilder(long id, java.math.BigDecimal amount, String note, LocalDate date) {
            this.id = id;
            this.amount = amount;
            this.note = note;
            this.date = date;
        }

        private Expense build() {
            return new Expense(id, amount, note, date, users);
        }
    }
}
