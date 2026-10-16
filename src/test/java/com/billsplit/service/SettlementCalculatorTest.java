package com.billsplit.service;

import com.billsplit.model.Expense;
import com.billsplit.model.Settlement;
import com.billsplit.model.TransactionShare;
import com.billsplit.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettlementCalculatorTest {
    private final SettlementCalculator calculator = new SettlementCalculator();

    @Test
    void minimizesTransfersFromLentAndBorrow() {
        List<User> members = List.of(
                new User(1, "Alice", "a@example.com"),
                new User(2, "Bob", "b@example.com"),
                new User(3, "Charlie", "c@example.com"));
        Expense dinner = new Expense(
                1L,
                new BigDecimal("90.00"),
                "Dinner",
                LocalDate.now(),
                List.of(
                        new TransactionShare(1, "Alice", BigDecimal.ZERO, new BigDecimal("90.00")),
                        new TransactionShare(2, "Bob", new BigDecimal("30.00"), BigDecimal.ZERO),
                        new TransactionShare(3, "Charlie", new BigDecimal("30.00"), BigDecimal.ZERO),
                        new TransactionShare(1, "Alice", new BigDecimal("30.00"), BigDecimal.ZERO)));

        List<Settlement> settlements = calculator.calculate(members, List.of(dinner));
        List<Settlement> payments = settlements.stream()
                .filter(s -> s.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .toList();
        assertEquals(2, payments.size());
        assertTrue(payments.stream().anyMatch(s -> s.getFrom().equals("Bob") && s.getTo().equals("Alice")));
        assertTrue(payments.stream().anyMatch(s -> s.getFrom().equals("Charlie") && s.getTo().equals("Alice")));
        BigDecimal total = payments.stream().map(Settlement::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("60.00"), total);
    }

    @Test
    void dummyZeroBalancesAreNotRealPayments() {
        List<User> members = List.of(
                new User(1, "Alice", "a@example.com"),
                new User(2, "Bob", "b@example.com"));
        List<Settlement> settlements = calculator.calculate(members, List.of());
        List<Settlement> payments = settlements.stream()
                .filter(s -> s.getAmount().compareTo(BigDecimal.ZERO) > 0 && !"No one".equals(s.getTo()))
                .toList();
        assertTrue(payments.isEmpty());
    }
}
