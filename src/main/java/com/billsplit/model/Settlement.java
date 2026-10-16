package com.billsplit.model;

import java.math.BigDecimal;

public final class Settlement {
    private final String from;
    private final String to;
    private final BigDecimal amount;
    private final boolean settled;

    public Settlement(String from, String to, BigDecimal amount, boolean settled) {
        this.from = from;
        this.to = to;
        this.amount = amount;
        this.settled = settled;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isSettled() {
        return settled;
    }
}
