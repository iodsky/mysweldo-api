package com.iodsky.sweldox.payroll.core;

import com.iodsky.sweldox.attendance.Attendance;
import com.iodsky.sweldox.attendance.AttendanceService;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.employee.EmployeeService;
import com.iodsky.sweldox.benefit.Benefit;
import com.iodsky.sweldox.payroll.deduction.Deduction;
import com.iodsky.sweldox.payroll.deduction.DeductionTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollBuilder {

    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final DeductionTypeRepository deductionTypeRepository;
    private final PayrollCalculator payrollCalculator;

    public Payroll buildPayroll(Long employeeId, LocalDate periodStart, LocalDate periodEnd, LocalDate payDate) {
        // Build context with all necessary data
        PayrollContext context = buildContext(employeeId, periodStart, periodEnd, payDate);

        // Build and return payroll entity
        return buildPayrollFromContext(context, payDate);
    }

    public Payroll buildPayroll(Long employeeId, LocalDate periodStart, LocalDate periodEnd, LocalDate payDate, PayrollConfiguration config) {
        PayrollContext context = buildContext(employeeId, periodStart, periodEnd, payDate, config);

        return buildPayrollFromContext(context, payDate);
    }

    private PayrollContext buildContext(Long employeeId, LocalDate periodStart, LocalDate periodEnd, LocalDate payDate) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        List<Attendance> attendances = attendanceService.getEmployeeAttendances(employeeId, periodStart, periodEnd);
        List<Benefit> benefits = employee.getBenefits();

        BigDecimal basicSalary = employee.getBasicSalary();
        BigDecimal hourlyRate = employee.getHourlyRate();

        // Calculate hours
        BigDecimal totalHours = payrollCalculator.calculateTotalHours(attendances);
        BigDecimal overtimeHours = payrollCalculator.calculateOvertimeHours(attendances);
        BigDecimal regularHours = totalHours.subtract(overtimeHours);

        // Calculate pay
        BigDecimal regularPay = payrollCalculator.calculateRegularPay(hourlyRate, regularHours);
        BigDecimal overtimePay = payrollCalculator.calculateOvertimePay(hourlyRate, overtimeHours);
        BigDecimal grossPay = payrollCalculator.calculateGrossPay(regularPay, overtimePay);

        // Calculate benefits
        BigDecimal totalBenefits = payrollCalculator.calculateTotalBenefits(benefits);

        // Calculate statutory deductions (using payDate for configuration lookup)
        BigDecimal sss = payrollCalculator.calculateSssDeduction(basicSalary, payDate);
        BigDecimal philhealth = payrollCalculator.calculatePhilhealthDeduction(basicSalary, payDate);
        BigDecimal pagibig = payrollCalculator.calculatePagibigDeduction(basicSalary, payDate);

        // Calculate tax
        BigDecimal statutoryDeductions = payrollCalculator.calculateTotalStatutoryDeductions(sss, philhealth, pagibig);
        BigDecimal taxableIncome = payrollCalculator.calculateTaxableIncome(grossPay, statutoryDeductions);
        BigDecimal withholdingTax = payrollCalculator.calculateWithholdingTax(taxableIncome, payDate);
        BigDecimal totalDeductions = withholdingTax.add(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);

        // Calculate net pay
        BigDecimal netPay = payrollCalculator.calculateNetPay(grossPay, totalBenefits, statutoryDeductions, withholdingTax);

        return PayrollContext.builder()
                .employeeId(employeeId)
                .employee(employee)
                .attendances(attendances)
                .benefits(benefits)
                .hourlyRate(hourlyRate)
                .basicSalary(basicSalary)
                .totalHours(totalHours)
                .overtimeHours(overtimeHours)
                .regularHours(regularHours)
                .regularPay(regularPay)
                .overtimePay(overtimePay)
                .grossPay(grossPay)
                .totalBenefits(totalBenefits)
                .sss(sss)
                .philhealth(philhealth)
                .pagibig(pagibig)
                .taxableIncome(taxableIncome)
                .withholdingTax(withholdingTax)
                .totalDeductions(totalDeductions)
                .netPay(netPay)
                .build();
    }

    /**
     * Build payroll context using pre-loaded configuration to avoid repeated database queries.
     * Use this method when processing multiple payrolls for the same period.
     */
    private PayrollContext buildContext(Long employeeId, LocalDate periodStart, LocalDate periodEnd, LocalDate payDate, PayrollConfiguration config) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        List<Attendance> attendances = attendanceService.getEmployeeAttendances(employeeId, periodStart, periodEnd);
        List<Benefit> benefits = employee.getBenefits();

        BigDecimal basicSalary = employee.getBasicSalary();
        BigDecimal hourlyRate = employee.getHourlyRate();

        // Calculate hours
        BigDecimal totalHours = payrollCalculator.calculateTotalHours(attendances);
        BigDecimal overtimeHours = payrollCalculator.calculateOvertimeHours(attendances);
        BigDecimal regularHours = totalHours.subtract(overtimeHours);

        // Calculate pay
        BigDecimal regularPay = payrollCalculator.calculateRegularPay(hourlyRate, regularHours);
        BigDecimal overtimePay = payrollCalculator.calculateOvertimePay(hourlyRate, overtimeHours);
        BigDecimal grossPay = payrollCalculator.calculateGrossPay(regularPay, overtimePay);

        // Calculate benefits
        BigDecimal totalBenefits = payrollCalculator.calculateTotalBenefits(benefits);

        // Calculate statutory deductions using preloaded configuration
        BigDecimal sss = payrollCalculator.calculateSssDeduction(basicSalary, payDate);
        BigDecimal philhealth = payrollCalculator.calculatePhilhealthDeduction(basicSalary, config.getPhilhealthContribution());
        BigDecimal pagibig = payrollCalculator.calculatePagibigDeduction(basicSalary, config.getPagibigContribution());

        // Calculate tax using pre-loaded configuration
        BigDecimal statutoryDeductions = payrollCalculator.calculateTotalStatutoryDeductions(sss, philhealth, pagibig);
        BigDecimal taxableIncome = payrollCalculator.calculateTaxableIncome(grossPay, statutoryDeductions);
        BigDecimal withholdingTax = payrollCalculator.calculateWithholdingTax(taxableIncome, config.getIncomeTaxBrackets());
        BigDecimal totalDeductions = withholdingTax.add(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);

        // Calculate net pay
        BigDecimal netPay = payrollCalculator.calculateNetPay(grossPay, totalBenefits, statutoryDeductions, withholdingTax);

        return PayrollContext.builder()
                .employeeId(employeeId)
                .employee(employee)
                .attendances(attendances)
                .benefits(benefits)
                .hourlyRate(hourlyRate)
                .basicSalary(basicSalary)
                .totalHours(totalHours)
                .overtimeHours(overtimeHours)
                .regularHours(regularHours)
                .regularPay(regularPay)
                .overtimePay(overtimePay)
                .grossPay(grossPay)
                .totalBenefits(totalBenefits)
                .sss(sss)
                .philhealth(philhealth)
                .pagibig(pagibig)
                .taxableIncome(taxableIncome)
                .withholdingTax(withholdingTax)
                .totalDeductions(totalDeductions)
                .netPay(netPay)
                .build();
    }

    private Payroll buildPayrollFromContext(PayrollContext context, LocalDate payDate) {
        BigDecimal dailyRate = payrollCalculator.calculateDailyRate(context.getHourlyRate());

        // Build deduction list
        List<Deduction> deductions = buildDeductions(context);

        // Build payroll benefits
        List<PayrollBenefit> payrollBenefits = buildPayrollBenefits(context.getBenefits());

        // Determine period dates from attendances
        LocalDate periodStartDate = context.getAttendances().isEmpty() ? null :
                context.getAttendances().getFirst().getDate();
        LocalDate periodEndDate = context.getAttendances().isEmpty() ? null :
                context.getAttendances().getLast().getDate();

        // Build payroll entity
        Payroll payroll = Payroll.builder()
                .employee(context.getEmployee())
                .monthlyRate(context.getBasicSalary())
                .dailyRate(dailyRate)
                .periodStartDate(periodStartDate)
                .periodEndDate(periodEndDate)
                .payDate(payDate)
                .daysWorked(context.getAttendances().size())
                .overtime(context.getOvertimeHours())
                .grossPay(context.getGrossPay())
                .benefits(payrollBenefits)
                .totalBenefits(context.getTotalBenefits())
                .deductions(deductions)
                .totalDeductions(context.getTotalDeductions())
                .netPay(context.getNetPay())
                .build();

        deductions.forEach(d -> d.setPayroll(payroll));
        payrollBenefits.forEach(b -> b.setPayroll(payroll));

        return payroll;
    }

    private List<Deduction> buildDeductions(PayrollContext context) {
        List<Deduction> deductions = new ArrayList<>();

        deductions.add(Deduction.builder()
                .deductionType(deductionTypeRepository.findByCode("SSS").orElseThrow())
                .amount(context.getSss())
                .build());

        deductions.add(Deduction.builder()
                .deductionType(deductionTypeRepository.findByCode("PHIC").orElseThrow())
                .amount(context.getPhilhealth())
                .build());

        deductions.add(Deduction.builder()
                .deductionType(deductionTypeRepository.findByCode("HDMF").orElseThrow())
                .amount(context.getPagibig())
                .build());

        deductions.add(Deduction.builder()
                .deductionType(deductionTypeRepository.findByCode("TAX").orElseThrow())
                .amount(context.getWithholdingTax())
                .build());

        return deductions;
    }

    private List<PayrollBenefit> buildPayrollBenefits(List<Benefit> benefits) {
        return benefits.stream()
                .map(benefit -> PayrollBenefit.builder()
                        .benefitType(benefit.getBenefitType())
                        .amount(benefit.getAmount())
                        .build())
                .toList();
    }

}
