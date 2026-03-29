package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PayrollContext {
    private Employee employee;
    private List<EmployeeBenefit> employeeBenefits;

    private BigDecimal monthlyRate;
    private BigDecimal semiMonthlyRate;
    private BigDecimal dailyRate;
    private BigDecimal hourlyRate;

    private BigDecimal daysWorked;
    private BigDecimal absenceDays;
    private Integer tardinessMinutes;
    private Integer undertimeMinutes;

    private BigDecimal totalHours;
    private BigDecimal overtimeHours;
    private BigDecimal regularHours;

    private BigDecimal absenceDeduction;
    private BigDecimal tardinessDeduction;
    private BigDecimal undertimeDeduction;

    private BigDecimal regularPay;
    private BigDecimal overtimePay;
    private BigDecimal grossPay;

    private BigDecimal totalBenefits;

    private BigDecimal sss;
    private BigDecimal philhealth;
    private BigDecimal pagibig;

    private BigDecimal sssEr;
    private BigDecimal philhealthEr;
    private BigDecimal pagibigEr;
    private BigDecimal totalEmployerContributions;

    private BigDecimal taxableIncome;
    private BigDecimal withholdingTax;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
}

