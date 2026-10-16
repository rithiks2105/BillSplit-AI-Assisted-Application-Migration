package com.billsplit.model;

import java.math.BigDecimal;

public final class TransactionShare {
    private final long userId;
    private final String name;
    private final BigDecimal borrow;
    private final BigDecimal lent;

    public TransactionShare(long userId, String name, BigDecimal borrow, BigDecimal lent) {
        this.userId = userId;
        this.name = name;
        this.borrow = borrow == null ? BigDecimal.ZERO : borrow;
        this.lent = lent == null ? BigDecimal.ZERO : lent;
    }

    public long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getBorrow() {
        return borrow;
    }

    public BigDecimal getLent() {
        return lent;
    }
}
