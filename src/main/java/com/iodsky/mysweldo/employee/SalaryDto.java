package com.iodsky.mysweldo.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SalaryDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal rate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayType payType;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollFrequency payFrequency;
}
