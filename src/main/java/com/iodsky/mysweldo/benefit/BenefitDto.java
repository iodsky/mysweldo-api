package com.iodsky.mysweldo.benefit;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean taxable;
    private BigDecimal nonTaxablelimit;
    private Instant createdAt;
    private Instant updatedAt;
}
