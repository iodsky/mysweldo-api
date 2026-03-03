package com.iodsky.mysweldo.payroll.contribution.pagIbig;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PagibigRateTableDto {
    private UUID id;
    private BigDecimal employeeRate;
    private BigDecimal employerRate;
    private BigDecimal lowIncomeThreshold;
    private BigDecimal lowIncomeEmployeeRate;
    private BigDecimal maxSalaryCap;
    private LocalDate effectiveDate;
    private Instant createdAt;
    private Instant updatedAt;
}
