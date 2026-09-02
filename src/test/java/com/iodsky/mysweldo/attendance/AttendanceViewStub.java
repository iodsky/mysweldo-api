package com.iodsky.mysweldo.attendance;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public class AttendanceViewStub implements AttendanceView {
    private UUID id;
    private String Employee_FirstName;
    private String Employee_LastName;
    private LocalDateTime timeIn;
    private LocalDateTime timeOut;
    private BigDecimal totalHours;

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getEmployee_FirstName() {
        return Employee_FirstName;
    }

    @Override
    public String getEmployee_LastName() {
        return Employee_LastName;
    }

    @Override
    public LocalDateTime getTimeIn() {
        return timeIn;
    }

    @Override
    public LocalDateTime getTimeOut() {
        return timeOut;
    }

    @Override
    public BigDecimal getTotalHours() {
        return totalHours;
    }
}