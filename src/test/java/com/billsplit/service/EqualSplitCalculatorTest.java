package com.billsplit.service;

import com.billsplit.model.TransactionShare;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EqualSplitCalculatorTest {
    private final EqualSplitCalculator calculator = new EqualSplitCalculator();

    @Test
    void splitsEvenlyWithRemainderCents() {
        List<TransactionShare> shares = calculator.split(new BigDecimal("10.00"), List.of(1L, 2L, 3L));
        BigDecimal total = shares.stream().map(TransactionShare::getBorrow).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(new BigDecimal("10.00"), total);
        assertEquals(3, shares.size());
        assertEquals(new BigDecimal("3.34"), shares.get(0).getBorrow());
        assertEquals(new BigDecimal("3.33"), shares.get(1).getBorrow());
        assertEquals(new BigDecimal("3.33"), shares.get(2).getBorrow());
    }

    @Test
    void requiresSelectedMembers() {
        assertThrows(AppException.class, () -> calculator.split(new BigDecimal("10.00"), List.of()));
    }

    @Test
    void splitsEvenlyWhenNoRemainder() {
        List<TransactionShare> shares = calculator.split(new BigDecimal("10.00"), List.of(1L, 2L));
        assertEquals(new BigDecimal("5.00"), shares.get(0).getBorrow());
        assertEquals(new BigDecimal("5.00"), shares.get(1).getBorrow());
    }
}
