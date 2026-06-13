package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.contribution.ContributionService;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.payroll.strategy.PayrollComputationStrategy;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates payroll computation for a single employee:
 * validates frequency, delegates to a PayrollComputationStrategy,
 * and assembles the resulting PayrollItem entity.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollItemAssembler {

    private final EmployeeService employeeService;
    private final DeductionService deductionService;
    private final ContributionService contributionService;
    private final PayrollComputationStrategy strategy;

    public PayrollItem buildPayroll(Long employeeId, PayrollRun run, StatutoryRateSnapshot rates) {
        Employee employee = employeeService.getEmployeeById(employeeId);

        PayrollFrequency runFrequency = run.getPeriod().getFrequency();
        PayrollFrequency employeeFrequency = employee.getSalary() != null
                ? employee.getSalary().getPayrollFrequency()
                : null;

        if (employeeFrequency == null) {
            log.warn("Employee {} has no payroll frequency set; assuming run frequency {}", employeeId, runFrequency);
        } else if (employeeFrequency != runFrequency) {
            throw new PayrollRunException(
                    "Employee " + employeeId + " payroll frequency " + employeeFrequency
                    + " does not match run frequency " + runFrequency
            );
        }

        PayrollComputationResult result = strategy.compute(employee, run, rates);
        return assemblePayrollItem(result, run);
    }

    private PayrollItem assemblePayrollItem(PayrollComputationResult result, PayrollRun payrollRun) {
        List<PayrollDeduction> deductions = buildDeductions(result);
        List<PayrollBenefit> payrollBenefits = buildPayrollBenefits(result.getEmployeeBenefits());
        List<EmployerContribution> employerContributions = buildEmployerContributions(result);

        int overtimeMinutes = result.getOvertimeHours().multiply(BigDecimal.valueOf(60)).setScale(2, RoundingMode.HALF_UP).intValue();

        PayrollItem payroll = PayrollItem.builder()
                .payrollRun(payrollRun)
                .employee(result.getEmployee())
                .payType(result.getPayType())
                .monthlyRate(result.getMonthlyRate())
                .semiMonthlyRate(result.getSemiMonthlyRate())
                .dailyRate(result.getDailyRate())
                .hourlyRate(result.getHourlyRate())
                .daysWorked(result.getDaysWorked())
                .absences(result.getAbsenceDays())
                .tardinessMinutes(result.getTardinessMinutes())
                .undertimeMinutes(result.getUndertimeMinutes())
                .overtimeMinutes(overtimeMinutes)
                .overtimePay(result.getOvertimePay())
                .grossPay(result.getGrossPay())
                .benefits(payrollBenefits)
                .totalBenefits(result.getTotalBenefits())
                .deductions(deductions)
                .totalDeductions(result.getTotalDeductions())
                .employerContributions(employerContributions)
                .netPay(result.getNetPay())
                .build();

        deductions.forEach(d -> d.setPayrollItem(payroll));
        payrollBenefits.forEach(b -> b.setPayrollItem(payroll));
        employerContributions.forEach(c -> c.setPayrollItem(payroll));

        return payroll;
    }

    private List<PayrollDeduction> buildDeductions(PayrollComputationResult result) {
        List<PayrollDeduction> deductions = new ArrayList<>();

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("SSS"))
                .amount(result.getSss())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("PHIC"))
                .amount(result.getPhilhealth())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("HDMF"))
                .amount(result.getPagibig())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("TAX"))
                .amount(result.getWithholdingTax())
                .build());

        return deductions;
    }

    private List<PayrollBenefit> buildPayrollBenefits(List<EmployeeBenefit> employeeBenefits) {
        return employeeBenefits.stream()
                .map(employeeBenefit -> PayrollBenefit.builder()
                        .benefit(employeeBenefit.getBenefit())
                        .amount(employeeBenefit.getAmount())
                        .build())
                .toList();
    }

    private List<EmployerContribution> buildEmployerContributions(PayrollComputationResult result) {
        List<EmployerContribution> contributions = new ArrayList<>();

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("SSS_ER"))
                .amount(result.getSssEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("PHIC_ER"))
                .amount(result.getPhilhealthEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("HDMF_ER"))
                .amount(result.getPagibigEr())
                .build());

        return contributions;
    }

}
