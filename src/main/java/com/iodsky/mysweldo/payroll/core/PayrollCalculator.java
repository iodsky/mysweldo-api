package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.pagIbig.PagibigRateRepository;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRateRepository;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.sss.SssRateRepository;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.tax.TaxBracket;
import com.iodsky.mysweldo.tax.TaxBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollCalculator {

    public static final BigDecimal STANDARD_WORK_HOURS_PER_DAY = BigDecimal.valueOf(8);
    private final PhilhealthRateRepository philhealthRateTableRepository;
    private final PagibigRateRepository pagibigRateTableRepository;
    private final SssRateRepository sssRateTableRepository;
    private final TaxBracketRepository incomeTaxBracketRepository;

    private static final BigDecimal SEMI_MONTHLY_PERIODS_PER_MONTH = BigDecimal.valueOf(2);
    public static final BigDecimal AVERAGE_WORKING_DAYS_PER_MONTH = BigDecimal.valueOf(21.75);
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.25);

    public PayrollConfiguration loadConfiguration(LocalDate payrollDate) {
        PhilhealthRate philhealth = philhealthRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "PhilHealth rate table not found for date: " + payrollDate
                ));

        PagibigRate pagibig = pagibigRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
            .orElseThrow(() -> new PayrollRunException(
                        "Pag-IBIG rate table not found for date: " + payrollDate
                ));

        SssRate sssRateTable = sssRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "SSS rate table not found for date: " + payrollDate
                ));

        List<TaxBracket> taxBrackets = incomeTaxBracketRepository
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

    public BigDecimal calculateSemiMonthlyRate(BigDecimal monthlyRate) {
        return monthlyRate.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDailyRate(BigDecimal monthlyRate) {
        return monthlyRate.divide(AVERAGE_WORKING_DAYS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateHourlyRate(BigDecimal dailyRate) {
        return dailyRate.divide(STANDARD_WORK_HOURS_PER_DAY, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateOvertimePay(BigDecimal hourlyRate, BigDecimal overtimeHours) {
        return hourlyRate
                .multiply(overtimeHours)
                .multiply(OVERTIME_MULTIPLIER);
    }

    public BigDecimal calculateTaxableBenefits(List<EmployeeBenefit> benefits) {
        return benefits.stream()
                .map(EmployeeBenefit::getTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNonTaxableBenefits(List<EmployeeBenefit> benefits) {
        return benefits.stream()
                .map(EmployeeBenefit::getNonTaxableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalBenefits(BigDecimal taxableBenefits, BigDecimal nonTaxableBenefits) {
        return taxableBenefits.add(nonTaxableBenefits).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateGrossPay(BigDecimal regularPay, BigDecimal overtimePay, BigDecimal taxableBenefits) {
        return regularPay.add(overtimePay)
                .add(taxableBenefits)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePhilhealthDeduction(BigDecimal basicSalary, PhilhealthRate config) {
        if (basicSalary.compareTo(config.getMinSalaryFloor()) <= 0) {
            BigDecimal employeeShare = config.getFixedContribution().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            return employeeShare.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
        }

        BigDecimal cappedSalary = basicSalary.min(config.getMaxSalaryCap());

        BigDecimal monthlyPremium = cappedSalary.multiply(config.getPremiumRate());

        BigDecimal employeeShare = monthlyPremium.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

        return employeeShare.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePagibigDeduction(BigDecimal basicSalary, PagibigRate config) {
        BigDecimal monthlySalary = basicSalary.min(config.getMaxSalaryCap());
        BigDecimal rate;

        if (monthlySalary.compareTo(config.getLowIncomeThreshold()) <= 0) {
            rate = config.getLowIncomeEmployeeRate();
        } else {
            rate = config.getEmployeeRate();
        }

        BigDecimal monthlyContribution = monthlySalary.multiply(rate);
        return monthlyContribution.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSssDeduction(BigDecimal basicSalary, SssRate sssRateTable) {
        // Find the appropriate salary bracket
        SssRate.SalaryBracket bracket = sssRateTable.findBracket(basicSalary);

        // Calculate the monthly contribution based on MSC
        BigDecimal monthlyContribution = bracket.getMsc().multiply(sssRateTable.getEmployeeRate());

        // Divide by 2 for semi-monthly payroll
        return monthlyContribution.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalStatutoryDeductions(BigDecimal sss, BigDecimal philhealth, BigDecimal pagibig) {
        return sss.add(philhealth).add(pagibig).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(BigDecimal semiMonthlyTaxableIncome, List<TaxBracket> taxBrackets) {

        BigDecimal monthlyTaxableIncome = semiMonthlyTaxableIncome.multiply(SEMI_MONTHLY_PERIODS_PER_MONTH);

        TaxBracket bracket = taxBrackets.stream()
                .filter(b -> monthlyTaxableIncome.compareTo(b.getMinIncome()) >= 0
                        && (b.getMaxIncome() == null
                        || monthlyTaxableIncome.compareTo(b.getMaxIncome()) <= 0))
                .findFirst()
                .orElseThrow(() -> new PayrollRunException(
                        "Income tax bracket not found for monthly income: " + monthlyTaxableIncome
                ));

        return calculateWithholdingTaxFromBracket(
                monthlyTaxableIncome,
                bracket
        );
    }

    private BigDecimal calculateWithholdingTaxFromBracket(BigDecimal monthlyEquivalent,  TaxBracket bracket) {
        BigDecimal excessAmount = monthlyEquivalent
                        .subtract(bracket.getThreshold())
                        .max(BigDecimal.ZERO);

        BigDecimal monthlyTax = bracket.getBaseTax()
                        .add(excessAmount.multiply(bracket.getMarginalRate()));

        return monthlyTax.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalDeductions(BigDecimal withholdingTax, BigDecimal totalStatutoryDeductions) {
        return withholdingTax.add(totalStatutoryDeductions).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateSssEmployerContribution(BigDecimal basicSalary, SssRate config) {
        SssRate.SalaryBracket bracket = config.findBracket(basicSalary);
        BigDecimal monthlyContribution = bracket.getMsc().multiply(config.getEmployerRate());
        return monthlyContribution.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePhilhealthEmployerContribution(BigDecimal basicSalary, PhilhealthRate config) {
        // PhilHealth is a split equally,  employer share equals employee share
        return calculatePhilhealthDeduction(basicSalary, config);
    }

    public BigDecimal calculatePagibigEmployerContribution(BigDecimal basicSalary, PagibigRate config) {
        // Employer always uses the flat employer_rate regardless of income tier
        BigDecimal monthlySalary = basicSalary.min(config.getMaxSalaryCap());
        BigDecimal monthlyContribution = monthlySalary.multiply(config.getEmployerRate());
        return monthlyContribution.divide(SEMI_MONTHLY_PERIODS_PER_MONTH, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalEmployerContributions(BigDecimal sssEr, BigDecimal philhealthEr, BigDecimal pagibigEr) {
        return sssEr.add(philhealthEr).add(pagibigEr).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTaxableIncome(BigDecimal grossPay, BigDecimal statutoryDeductions) {
        return grossPay.subtract(statutoryDeductions).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateAbsenceDeduction(BigDecimal dailyRate, BigDecimal absenceDays) {
        return dailyRate.multiply(absenceDays).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTardinessDeduction(BigDecimal hourlyRate, Integer tardinessMinutes) {
        if (tardinessMinutes == null || tardinessMinutes == 0) {
            return BigDecimal.ZERO;
        }

        return hourlyRate
                .multiply(BigDecimal.valueOf(tardinessMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateUndertimeDeduction(BigDecimal hourlyRate, Integer undertimeMinutes) {
        if (undertimeMinutes == null || undertimeMinutes == 0) {
            return BigDecimal.ZERO;
        }

        return hourlyRate
                .multiply(BigDecimal.valueOf(undertimeMinutes))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateRegularPay(BigDecimal semiMonthlyRate, BigDecimal absenceDeduction, BigDecimal tardinessDeduction, BigDecimal undertimeDeduction) {
        return semiMonthlyRate
                .subtract(absenceDeduction)
                .subtract(tardinessDeduction)
                .subtract(undertimeDeduction)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateNetPay( BigDecimal grossPay, BigDecimal nonTaxableBenefits, BigDecimal statutoryDeductions, BigDecimal withholdingTax) {
        return grossPay.add(nonTaxableBenefits)
                .subtract(statutoryDeductions)
                .subtract(withholdingTax)
                .setScale(2, RoundingMode.HALF_UP);
    }

}