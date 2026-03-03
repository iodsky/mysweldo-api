package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.benefit.Benefit;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class PayrollContext {
    private Employee employee;
    private List<Attendance> attendances;
    private List<Benefit> benefits;

    private BigDecimal hourlyRate;
    private BigDecimal basicSalary;

    private BigDecimal totalHours;
    private BigDecimal overtimeHours;
    private BigDecimal regularHours;

    private BigDecimal regularPay;
    private BigDecimal overtimePay;
    private BigDecimal grossPay;

    private BigDecimal totalBenefits;

    private BigDecimal sss;
    private BigDecimal philhealth;
    private BigDecimal pagibig;

    private BigDecimal taxableIncome;
    private BigDecimal withholdingTax;
    private BigDecimal totalDeductions;
    private BigDecimal netPay;
}

