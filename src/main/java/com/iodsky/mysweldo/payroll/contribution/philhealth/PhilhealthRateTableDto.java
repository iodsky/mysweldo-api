package com.iodsky.mysweldo.payroll.contribution.philhealth;

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
public class PhilhealthRateTableDto {
    private UUID id;
    private BigDecimal premiumRate;
    private BigDecimal maxSalaryCap;
    private BigDecimal minSalaryFloor;
    private BigDecimal fixedContribution;
    private LocalDate effectiveDate;
    private Instant createdAt;
    private Instant updatedAt;
}
