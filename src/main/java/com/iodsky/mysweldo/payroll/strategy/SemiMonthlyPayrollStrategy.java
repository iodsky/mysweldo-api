package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import com.iodsky.mysweldo.payroll.core.PayrollConfiguration;
import com.iodsky.mysweldo.payroll.core.PayrollContext;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.overtime.OvertimeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Strategy for computing SEMI_MONTHLY payroll.
 *
 * Responsibilities:
 * - Fetch required data for the payroll period
 * - Orchestrate calculations using PayrollCalculator
 * - Build and return a PayrollContext with all computed values
 */
@Component
@RequiredArgsConstructor
public class SemiMonthlyPayrollStrategy implements PayrollComputationStrategy {

    private final AttendanceService attendanceService;
    private final OvertimeRequestService overtimeRequestService;
    private final PayrollCalculator payrollCalculator;

    /**
     * Computes payroll for a SEMI_MONTHLY employee within a given payroll run.
     *
     * @param employee Employee to compute payroll for
     * @param payrollRun The current payroll run
     * @param config Payroll configuration (statutory rates, tax brackets)
     * @return PayrollContext containing all calculated fields
     */
    @Override
    public PayrollContext compute(Employee employee, PayrollRun payrollRun, PayrollConfiguration config) {
        // 1. FETCH REQUIRED DATA
        List<Attendance> attendances = attendanceService.getEmployeeAttendances(
                employee.getId(),
                payrollRun.getPeriodStartDate(),
                payrollRun.getPeriodEndDate()
        );

        List<EmployeeBenefit> benefits = employee.getBenefits();

        // Fetch salary information from the related Salary entity
        if (employee.getSalary() == null) {
            throw new PayrollRunException(
                    "No salary record found for employee: " + employee.getId()
            );
        }

        BigDecimal monthlyRate = employee.getSalary().getRate();
        BigDecimal semiMonthlyRate = payrollCalculator.calculateSemiMonthlyRate(monthlyRate);
        BigDecimal dailyRate = payrollCalculator.calculateDailyRate(monthlyRate);
        BigDecimal hourlyRate = payrollCalculator.calculateHourlyRate(dailyRate);

        // 2. CALCULATE HOURS
        BigDecimal totalHours = attendanceService.calculateTotalHoursByEmployeeId(
                employee.getId(),
                payrollRun.getPeriodStartDate(),
                payrollRun.getPeriodEndDate()
        );

        BigDecimal approvedOvertimeHours = overtimeRequestService.calculateApprovedOvertimeHours(
                employee.getId(),
                payrollRun.getPeriodStartDate(),
                payrollRun.getPeriodEndDate()
        );

        BigDecimal standardHours = BigDecimal.valueOf(attendances.size()).multiply(BigDecimal.valueOf(8));
        BigDecimal regularHours = totalHours.subtract(approvedOvertimeHours).min(standardHours);

        // 3. CALCULATE PAY
        BigDecimal overtimePay = payrollCalculator.calculateOvertimePay(hourlyRate, approvedOvertimeHours);
        BigDecimal grossPay = payrollCalculator.calculateGrossPay(semiMonthlyRate, overtimePay);

        // 4. CALCULATE BENEFITS
        BigDecimal totalBenefits = payrollCalculator.calculateTotalBenefits(benefits);

        // 5. CALCULATE STATUTORY DEDUCTIONS (using preloaded configuration)
        BigDecimal sss = payrollCalculator.calculateSssDeduction(monthlyRate, config.getSssRateTable());
        BigDecimal philhealth = payrollCalculator.calculatePhilhealthDeduction(monthlyRate, config.getPhilhealthRateTable());
        BigDecimal pagibig = payrollCalculator.calculatePagibigDeduction(monthlyRate, config.getPagibigRateTable());

        // 6. CALCULATE EMPLOYER CONTRIBUTIONS (using preloaded configuration)
        BigDecimal sssEr = payrollCalculator.calculateSssEmployerContribution(monthlyRate, config.getSssRateTable());
        BigDecimal philhealthEr = payrollCalculator.calculatePhilhealthEmployerContribution(monthlyRate, config.getPhilhealthRateTable());
        BigDecimal pagibigEr = payrollCalculator.calculatePagibigEmployerContribution(monthlyRate, config.getPagibigRateTable());
        BigDecimal totalEmployerContributions = payrollCalculator.calculateTotalEmployerContributions(sssEr, philhealthEr, pagibigEr);

        // 7. CALCULATE TAXABLE INCOME AND WITHHOLDING TAX (using preloaded configuration)
        BigDecimal statutoryDeductions = payrollCalculator.calculateTotalStatutoryDeductions(sss, philhealth, pagibig);
        BigDecimal taxableIncome = payrollCalculator.calculateTaxableIncome(grossPay, statutoryDeductions);
        BigDecimal withholdingTax = payrollCalculator.calculateWithholdingTax(taxableIncome, config.getIncomeTaxBrackets());
        BigDecimal totalDeductions = withholdingTax.add(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);

        // 8. CALCULATE NET PAY
        BigDecimal netPay = payrollCalculator.calculateNetPay(grossPay, totalBenefits, statutoryDeductions, withholdingTax);

        // 9. BUILD AND RETURN CONTEXT
        return PayrollContext.builder()
                .employee(employee)
                .attendances(attendances)
                .employeeBenefits(benefits)
                .monthlyRate(monthlyRate)
                .semiMonthlyRate(semiMonthlyRate)
                .dailyRate(dailyRate)
                .hourlyRate(hourlyRate)
                .totalHours(totalHours)
                .overtimeHours(approvedOvertimeHours)
                .regularHours(regularHours)
                .regularPay(semiMonthlyRate)
                .overtimePay(overtimePay)
                .grossPay(grossPay)
                .totalBenefits(totalBenefits)
                .sss(sss)
                .philhealth(philhealth)
                .pagibig(pagibig)
                .sssEr(sssEr)
                .philhealthEr(philhealthEr)
                .pagibigEr(pagibigEr)
                .totalEmployerContributions(totalEmployerContributions)
                .taxableIncome(taxableIncome)
                .withholdingTax(withholdingTax)
                .totalDeductions(totalDeductions)
                .netPay(netPay)
                .build();
    }
}
