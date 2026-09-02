package com.iodsky.mysweldo.tax;

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
public class TaxBracketDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal minIncome;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal maxIncome;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal baseTax;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal marginalRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal threshold;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    private Instant updatedAt;
}
