package com.iodsky.mysweldo.payroll.item;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollItemDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long employeeId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String employeeName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String designation;

    // Payroll period
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodStartDate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate periodEndDate;

    // Work & time
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal daysWorked;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal absences;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer tardinessMinutes;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer undertimeMinutes;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer overtimeMinutes;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal overtimePay;

    // Rates
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monthlyRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal semiMonthlyRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal dailyRate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal hourlyRate;

    // Payroll amounts
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalBenefits;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal grossPay;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal totalDeductions;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal netPay;

    // Related entities
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PayrollBenefitDto> benefits;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PayrollDeductionDto> deductions;

}

