package com.billsplit.service;

import com.billsplit.model.Expense;
import com.billsplit.model.TransactionShare;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseValidatorTest {
    private final ExpenseValidator validator = new ExpenseValidator();

    @Test
    void acceptsBalancedExpenseAmongTripMembers() {
        Expense expense = new Expense(
                null,
                new BigDecimal("50.00"),
                "Dinner",
                LocalDate.now(),
                List.of(
                        new TransactionShare(1, "Alice", BigDecimal.ZERO, new BigDecimal("50.00")),
                        new TransactionShare(2, "Bob", new BigDecimal("25.00"), BigDecimal.ZERO),
                        new TransactionShare(3, "Charlie", new BigDecimal("25.00"), BigDecimal.ZERO)));
        assertDoesNotThrow(() -> validator.validate(expense, Set.of(1L, 2L, 3L)));
    }

    @Test
    void rejectsMismatchedPaidAndSplit() {
        Expense expense = new Expense(
                null,
                new BigDecimal("50.00"),
                "Dinner",
                LocalDate.now(),
                List.of(
                        new TransactionShare(1, "Alice", BigDecimal.ZERO, new BigDecimal("50.00")),
                        new TransactionShare(2, "Bob", new BigDecimal("20.00"), BigDecimal.ZERO)));
        AppException ex = assertThrows(AppException.class, () -> validator.validate(expense, Set.of(1L, 2L)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void rejectsBlankNote() {
        Expense expense = new Expense(
                null,
                new BigDecimal("10.00"),
                "  ",
                LocalDate.now(),
                List.of(new TransactionShare(1, "Alice", new BigDecimal("10.00"), new BigDecimal("10.00"))));
        assertThrows(AppException.class, () -> validator.validate(expense, Set.of(1L)));
    }

    @Test
    void rejectsNonMember() {
        Expense expense = new Expense(
                null,
                new BigDecimal("10.00"),
                "Taxi",
                LocalDate.now(),
                List.of(
                        new TransactionShare(1, "Alice", BigDecimal.ZERO, new BigDecimal("10.00")),
                        new TransactionShare(99, "Eve", new BigDecimal("10.00"), BigDecimal.ZERO)));
        AppException ex = assertThrows(AppException.class, () -> validator.validate(expense, Set.of(1L)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void rejectsNoteLongerThanOracleColumn() {
        Expense expense = new Expense(
                null,
                new BigDecimal("10.00"),
                "x".repeat(1001),
                LocalDate.now(),
                List.of(new TransactionShare(1, "Alice", new BigDecimal("10.00"), new BigDecimal("10.00"))));
        AppException ex = assertThrows(AppException.class, () -> validator.validate(expense, Set.of(1L)));
        assertEquals(400, ex.getStatus());
    }
}
