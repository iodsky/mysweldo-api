package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SalaryHistoryDto {
    private UUID id;
    private BigDecimal rate;
    private PayType payType;
    private PayrollFrequency payFrequency;
    private LocalDate effectiveFrom;
    private Instant createdAt;
}