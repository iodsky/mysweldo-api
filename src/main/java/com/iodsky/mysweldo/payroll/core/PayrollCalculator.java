package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.batch.employee.EmployeeBenefit;
import com.iodsky.mysweldo.pagIbig.PagibigRateTable;
import com.iodsky.mysweldo.pagIbig.PagibigRateTableRepository;
import com.iodsky.mysweldo.philhealth.PhilhealthRateTable;
import com.iodsky.mysweldo.philhealth.PhilhealthRateTableRepository;
import com.iodsky.mysweldo.sss.SssRateTable;
import com.iodsky.mysweldo.sss.SssRateTableRepository;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.tax.IncomeTaxBracket;
import com.iodsky.mysweldo.tax.IncomeTaxBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollCalculator {

    private final PhilhealthRateTableRepository philhealthRateTableRepository;
    private final PagibigRateTableRepository pagibigRateTableRepository;
    private final SssRateTableRepository sssRateTableRepository;
    private final IncomeTaxBracketRepository incomeTaxBracketRepository;

    private static final BigDecimal SEMI_MONTHLY_DIVISOR = BigDecimal.valueOf(2);
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.25);
    private static final BigDecimal STANDARD_WORK_HOURS = BigDecimal.valueOf(8);
    private static final BigDecimal PAY_PERIODS_PER_YEAR = BigDecimal.valueOf(24);

    public PayrollConfiguration loadConfiguration(LocalDate payrollDate) {
        PhilhealthRateTable philhealth = philhealthRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "PhilHealth rate table not found for date: " + payrollDate
                ));

        PagibigRateTable pagibig = pagibigRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
            .orElseThrow(() -> new PayrollRunException(
                        "Pag-IBIG rate table not found for date: " + payrollDate
                ));

        SssRateTable sssRateTable = sssRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "SSS rate table not found for date: " + payrollDate
                ));

        List<IncomeTaxBracket> taxBrackets = incomeTaxBracketRepository
                .findAllByEffectiveDate(payrollDate);

        if (taxBrackets.isEmpty()) {
            throw new PayrollRunException(
                    "Income tax bracket configurations not found for date: " + payrollDate
            );
        }

        return PayrollConfiguration.builder()
                .philhealthRateTable(philhealth)
                .pagibigRateTable(pagibig)
                .sssRateTable(sssRateTable)
                .incomeTaxBrackets(taxBrackets)
                .build();
    }

    public BigDecimal calculateDailyRate(BigDecimal hourlyRate) {
        return hourlyRate.multiply(STANDARD_WORK_HOURS);
    }

    public BigDecimal calculateRegularPay(BigDecimal hourlyRate, BigDecimal regularHours) {
        return hourlyRate.multiply(regularHours);
    }

    public BigDecimal calculateOvertimePay(BigDecimal hourlyRate, BigDecimal overtimeHours) {
        return hourlyRate
                .multiply(overtimeHours)
                .multiply(OVERTIME_MULTIPLIER);
    }

    public BigDecimal calculateGrossPay(BigDecimal regularPay, BigDecimal overtimePay) {
        return regularPay.add(overtimePay).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalBenefits(List<EmployeeBenefit> employeeBenefits) {
        return employeeBenefits.stream()
                .map(EmployeeBenefit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal calculatePhilhealthDeduction(BigDecimal basicSalary, PhilhealthRateTable config) {
        if (basicSalary.compareTo(config.getMinSalaryFloor()) <= 0) {
            BigDecimal employeeShare = config.getFixedContribution().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return employeeShare.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
        }

        BigDecimal cappedSalary = basicSalary.min(config.getMaxSalaryCap());

        BigDecimal monthlyPremium = cappedSalary.multiply(config.getPremiumRate());

        BigDecimal employeeShare = monthlyPremium.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        return employeeShare.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePagibigDeduction(BigDecimal basicSalary, PagibigRateTable config) {
        BigDecimal monthlySalary = basicSalary.min(config.getMaxSalaryCap());
        BigDecimal rate;

        if (monthlySalary.compareTo(config.getLowIncomeThreshold()) <= 0) {
            rate = config.getLowIncomeEmployeeRate();
        } else {
            rate = config.getEmployeeRate();
        }

        BigDecimal monthlyContribution = monthlySalary.multiply(rate);
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSssDeduction(BigDecimal basicSalary, SssRateTable sssRateTable) {
        // Find the appropriate salary bracket
        SssRateTable.SalaryBracket bracket = sssRateTable.findBracket(basicSalary);

        // Calculate the monthly contribution based on MSC
        BigDecimal monthlyContribution = bracket.getMsc().multiply(sssRateTable.getEmployeeRate());

        // Divide by 2 for semi-monthly payroll
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(
            BigDecimal semiMonthlyTaxableIncome,
            List<IncomeTaxBracket> taxBrackets) {
        BigDecimal annualTaxableIncome =
                semiMonthlyTaxableIncome.multiply(PAY_PERIODS_PER_YEAR);

        IncomeTaxBracket bracket = taxBrackets.stream()
                .filter(b -> annualTaxableIncome.compareTo(b.getMinIncome()) >= 0
                        && (b.getMaxIncome() == null
                        || annualTaxableIncome.compareTo(b.getMaxIncome()) <= 0))
                .findFirst()
                .orElseThrow(() -> new PayrollRunException(
                        "Income tax bracket not found for annual income: " + annualTaxableIncome
                ));

        return calculateWithholdingTaxFromBracket(
                annualTaxableIncome,
                bracket
        );
    }

    private BigDecimal calculateWithholdingTaxFromBracket(
            BigDecimal annualTaxableIncome,
            IncomeTaxBracket annualBracket) {

        BigDecimal excessAmount =
                annualTaxableIncome
                        .subtract(annualBracket.getThreshold())
                        .max(BigDecimal.ZERO);

        BigDecimal annualTax =
                annualBracket.getBaseTax()
                        .add(excessAmount.multiply(annualBracket.getMarginalRate()));

        return annualTax.divide(PAY_PERIODS_PER_YEAR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSssEmployerContribution(BigDecimal basicSalary, LocalDate payrollDate) {
        SssRateTable config = sssRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "SSS rate table not found for date: " + payrollDate
                ));
        return calculateSssEmployerContribution(basicSalary, config);
    }

    public BigDecimal calculateSssEmployerContribution(BigDecimal basicSalary, SssRateTable config) {
        SssRateTable.SalaryBracket bracket = config.findBracket(basicSalary);
        BigDecimal monthlyContribution = bracket.getMsc().multiply(config.getEmployerRate());
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePhilhealthEmployerContribution(BigDecimal basicSalary, LocalDate payrollDate) {
        PhilhealthRateTable config = philhealthRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "PhilHealth rate table not found for date: " + payrollDate
                ));
        return calculatePhilhealthEmployerContribution(basicSalary, config);
    }

    public BigDecimal calculatePhilhealthEmployerContribution(BigDecimal basicSalary, PhilhealthRateTable config) {
        // PhilHealth is a split equally,  employer share equals employee share
        return calculatePhilhealthDeduction(basicSalary, config);
    }

    public BigDecimal calculatePagibigEmployerContribution(BigDecimal basicSalary, PagibigRateTable config) {
        // Employer always uses the flat employer_rate regardless of income tier
        BigDecimal monthlySalary = basicSalary.min(config.getMaxSalaryCap());
        BigDecimal monthlyContribution = monthlySalary.multiply(config.getEmployerRate());
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalEmployerContributions(
            BigDecimal sssEr, BigDecimal philhealthEr, BigDecimal pagibigEr) {
        return sssEr.add(philhealthEr).add(pagibigEr).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalStatutoryDeductions(
            BigDecimal sss, BigDecimal philhealth, BigDecimal pagibig) {
        return sss.add(philhealth).add(pagibig).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTaxableIncome(BigDecimal grossPay, BigDecimal statutoryDeductions) {
        return grossPay.subtract(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetPay(
            BigDecimal grossPay, BigDecimal totalBenefits,
            BigDecimal statutoryDeductions, BigDecimal withholdingTax) {
        return grossPay.add(totalBenefits)
                .subtract(statutoryDeductions)
                .subtract(withholdingTax)
                .setScale(2, RoundingMode.HALF_UP);
    }

}