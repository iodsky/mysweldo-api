package com.iodsky.mysweldo.philhealth;

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
public class PhilhealthRateDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal premiumRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal maxSalaryCap;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal minSalaryFloor;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal fixedContribution;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
    private Instant updatedAt;
}
