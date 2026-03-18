package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.employee.SalaryType;
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
    private String employeeName;
    private String designation;

    // Payroll period
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;

    // Work & time
    private BigDecimal daysWorked;
    private BigDecimal absences;
    private Integer tardinessMinutes;
    private Integer undertimeMinutes;
    private Integer overtimeMinutes;
    private BigDecimal overtimePay;

    // Rates
    private BigDecimal monthlyRate;
    private BigDecimal semiMonthlyRate;
    private BigDecimal dailyRate;
    private BigDecimal hourlyRate;
    private SalaryType salaryType;

    // Payroll amounts
    private BigDecimal totalBenefits;
    private BigDecimal grossPay;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;

    // Related entities
    private List<PayrollBenefitDto> benefits;
    private List<PayrollDeductionDto> deductions;

}

