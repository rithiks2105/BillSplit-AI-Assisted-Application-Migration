package com.billsplit.model;

import java.time.LocalDateTime;

public final class Trip {
    private final long id;
    private final String name;
    private final LocalDateTime startDate;

    public Trip(long id, String name, LocalDateTime startDate) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }
}
