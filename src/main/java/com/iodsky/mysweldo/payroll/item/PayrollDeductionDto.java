package com.iodsky.mysweldo.payroll.item;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollDeductionDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String deduction;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

}
