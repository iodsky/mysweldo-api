package com.iodsky.mysweldo.attendance;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface AttendanceView {
    UUID getId();
    String getEmployee_FirstName();
    String getEmployee_LastName();
    LocalDateTime getTimeIn();
    LocalDateTime getTimeOut();
    BigDecimal getTotalHours();
}