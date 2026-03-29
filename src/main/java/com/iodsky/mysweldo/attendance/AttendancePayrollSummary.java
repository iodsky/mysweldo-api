package com.iodsky.mysweldo.attendance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AttendancePayrollSummary {

    public BigDecimal daysWorked;
    public BigDecimal absenceDays;
    public Integer tardinessMinutes;
    public Integer undertimeMinutes;

}
