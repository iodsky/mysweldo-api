package com.iodsky.mysweldo.common;


import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public record DateRange(LocalDate startDate, LocalDate endDate) {
    public DateRange {
        if (startDate == null) {
            startDate = LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());
        }
        if (endDate == null) {
            endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }
}
