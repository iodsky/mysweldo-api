package com.iodsky.mysweldo.payroll.item;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollBenefitDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String benefit;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;
}
