package com.iodsky.mysweldo.payroll.core;

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

    private UUID id;
    private Long employeeId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private int daysWorked;
    private BigDecimal overtime;
    private BigDecimal monthlyRate;
    private BigDecimal dailyRate;
    private BigDecimal grossPay;
    private BigDecimal netPay;

    private List<PayrollBenefitDto> benefits;
    private List<PayrollDeductionDto> deductions;
    private List<EmployerContributionDto> employerContributions;

}

