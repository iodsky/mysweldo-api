package com.iodsky.mysweldo.pagIbig;

import io.swagger.v3.oas.annotations.media.Schema;

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
public class PagibigRateDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal employeeRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal employerRate;
    private BigDecimal lowIncomeThreshold;
    private BigDecimal lowIncomeEmployeeRate;
    private BigDecimal maxSalaryCap;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    private Instant updatedAt;
}
