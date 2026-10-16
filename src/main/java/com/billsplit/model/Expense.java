package com.billsplit.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class Expense {
    private final Long id;
    private final BigDecimal amount;
    private final String note;
    private final LocalDate date;
    private final List<TransactionShare> users;

    public Expense(Long id, BigDecimal amount, String note, LocalDate date, List<TransactionShare> users) {
        this.id = id;
        this.amount = amount;
        this.note = note;
        this.date = date;
        this.users = List.copyOf(users);
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getDate() {
        return date;
    }

    public List<TransactionShare> getUsers() {
        return users;
    }
}
