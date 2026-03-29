package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.contribution.ContributionService;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.payroll.strategy.PayrollComputationStrategy;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.strategy.PayrollStrategyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * PayrollBuilder is the orchestrator that coordinates the payroll computation process.
 *
 * Responsibilities:
 * 1. Fetch employee data
 * 2. Delegate calculations to an appropriate PayrollComputationStrategy
 * 3. Construct PayrollContext from computed values
 * 4. Build final PayrollItem entity with all related entities
 *
 * This design separates application logic (data orchestration) from business logic (calculations).
 */
@Component
@RequiredArgsConstructor
public class PayrollBuilder {

    private final EmployeeService employeeService;
    private final DeductionService deductionService;
    private final ContributionService contributionService;
    private final PayrollStrategyFactory strategyFactory;

    /**
     * Builds a complete PayrollItem for an employee within a payroll run.
     *
     * @param employeeId ID of the employee
     * @param run The payroll run
     * @param config Payroll configuration (rates, tax brackets)
     * @return A complete PayrollItem with all deductions, benefits, and contributions
     */
    public PayrollItem buildPayroll(Long employeeId, PayrollRun run, PayrollConfiguration config) {
        Employee employee = employeeService.getEmployeeById(employeeId);
        PayrollComputationStrategy strategy = strategyFactory.getStrategy(run.getPayrollFrequency());
        PayrollContext context = strategy.compute(employee, run, config);
        return buildPayrollFromContext(context, run);
    }

    /**
     * Transforms a PayrollContext into a persistable PayrollItem entity.
     *
     * @param context The computation snapshot containing all calculated values
     * @param payrollRun The associated payroll run
     * @return A complete PayrollItem with all related entities
     */
    private PayrollItem buildPayrollFromContext(PayrollContext context, PayrollRun payrollRun) {
        List<PayrollDeduction> deductions = buildDeductions(context);
        List<PayrollBenefit> payrollBenefits = buildPayrollBenefits(context.getEmployeeBenefits());
        List<EmployerContribution> employerContributions = buildEmployerContributions(context);

        PayrollItem payroll = PayrollItem.builder()
                .payrollRun(payrollRun)
                .employee(context.getEmployee())
                .monthlyRate(context.getMonthlyRate())
                .semiMonthlyRate(context.getSemiMonthlyRate())
                .dailyRate(context.getDailyRate())
                .hourlyRate(context.getHourlyRate())
                .daysWorked(context.getDaysWorked())
                .absences(context.getAbsenceDays())
                .tardinessMinutes(context.getTardinessMinutes())
                .undertimeMinutes(context.getUndertimeMinutes())
                .grossPay(context.getGrossPay())
                .benefits(payrollBenefits)
                .totalBenefits(context.getTotalBenefits())
                .deductions(deductions)
                .totalDeductions(context.getTotalDeductions())
                .employerContributions(employerContributions)
                .netPay(context.getNetPay())
                .build();

        deductions.forEach(d -> d.setPayrollItem(payroll));
        payrollBenefits.forEach(b -> b.setPayrollItem(payroll));
        employerContributions.forEach(c -> c.setPayrollItem(payroll));

        return payroll;
    }

    private List<PayrollDeduction> buildDeductions(PayrollContext context) {
        List<PayrollDeduction> deductions = new ArrayList<>();

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("SSS"))
                .amount(context.getSss())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("PHIC"))
                .amount(context.getPhilhealth())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("HDMF"))
                .amount(context.getPagibig())
                .build());

        deductions.add(PayrollDeduction.builder()
                .deduction(deductionService.getDeductionByCode("TAX"))
                .amount(context.getWithholdingTax())
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

    private List<EmployerContribution> buildEmployerContributions(PayrollContext context) {
        List<EmployerContribution> contributions = new ArrayList<>();

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("SSS_ER"))
                .amount(context.getSssEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("PHIC_ER"))
                .amount(context.getPhilhealthEr())
                .build());

        contributions.add(EmployerContribution.builder()
                .contribution(contributionService.getContributionByCode("HDMF_ER"))
                .amount(context.getPagibigEr())
                .build());

        return contributions;
    }

}
