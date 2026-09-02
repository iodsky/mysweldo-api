package com.iodsky.mysweldo.sss;

import io.swagger.v3.oas.annotations.media.Schema;

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
public class SssRateDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalSss;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal employeeSss;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal employerSss;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<SalaryBracketDto> salaryBrackets;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalaryBracketDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal minSalary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal maxSalary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal msc;
    }
}
