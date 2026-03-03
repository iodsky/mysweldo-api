package com.iodsky.mysweldo.payroll.contribution.sss;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SssRateTableDto {
    private UUID id;
    private BigDecimal totalSss;
    private BigDecimal employeeSss;
    private BigDecimal employerSss;
    private List<SalaryBracketDto> salaryBrackets;
    private LocalDate effectiveDate;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalaryBracketDto {
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private BigDecimal msc;
    }
}
