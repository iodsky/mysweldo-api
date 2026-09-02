package com.iodsky.mysweldo.payroll.run;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayrollRunDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodStartDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodEndDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollFrequency payrollFrequency;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollRunType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollRunStatus status;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalBenefits;
    private BigDecimal totalEmployerCost;
    private String notes;

}
