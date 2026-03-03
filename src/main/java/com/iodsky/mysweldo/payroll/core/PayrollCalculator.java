package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.payroll.contribution.pagIbig.PagibigRateTable;
import com.iodsky.mysweldo.payroll.contribution.pagIbig.PagibigRateTableRepository;
import com.iodsky.mysweldo.payroll.contribution.philhealth.PhilhealthRateTable;
import com.iodsky.mysweldo.payroll.contribution.philhealth.PhilhealthRateTableRepository;
import com.iodsky.mysweldo.payroll.contribution.sss.SssRateTable;
import com.iodsky.mysweldo.payroll.contribution.sss.SssRateTableRepository;
import com.iodsky.mysweldo.payroll.tax.IncomeTaxBracket;
import com.iodsky.mysweldo.payroll.tax.IncomeTaxBracketRepository;
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

    public BigDecimal calculateTotalBenefits(List<Benefit> benefits) {
        return benefits.stream()
                .map(Benefit::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculatePhilhealthDeduction(BigDecimal basicSalary, LocalDate payrollDate) {
        PhilhealthRateTable config = philhealthRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "PhilHealth rate table not found for date: " + payrollDate
                ));

        return calculatePhilhealthDeduction(basicSalary, config);
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

    public BigDecimal calculatePagibigDeduction(BigDecimal basicSalary, LocalDate payrollDate) {
        PagibigRateTable config = pagibigRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "Pag-IBIG rate table not found for date: " + payrollDate
                ));

        return calculatePagibigDeduction(basicSalary, config);
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

    public BigDecimal calculateSssDeduction(BigDecimal basicSalary, LocalDate payrollDate) {
        SssRateTable config = sssRateTableRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new PayrollRunException(
                        "SSS rate table not found for date: " + payrollDate
                ));

        return calculateSssDeduction(basicSalary, config);
    }

    public BigDecimal calculateSssDeduction(BigDecimal basicSalary, SssRateTable sssRateTable) {
        // Find the appropriate salary bracket
        SssRateTable.SalaryBracket bracket = sssRateTable.findBracket(basicSalary);

        // Calculate the monthly contribution based on MSC
        BigDecimal monthlyContribution = bracket.getMsc().multiply(sssRateTable.getEmployeeRate());

        // Divide by 2 for semi-monthly payroll
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(BigDecimal semiMonthlyTaxableIncome, LocalDate payrollDate) {
        // Convert semi-monthly taxable income to annual for bracket lookup
        BigDecimal annualTaxableIncome = semiMonthlyTaxableIncome.multiply(PAY_PERIODS_PER_YEAR);

        IncomeTaxBracket bracket = incomeTaxBracketRepository
                .findByIncomeAndEffectiveDate(annualTaxableIncome, payrollDate);

        if (bracket == null) {
            throw new PayrollRunException(
                    "Income tax bracket not found for annual income: " + annualTaxableIncome + " and date: " + payrollDate
            );
        }

        return calculateWithholdingTaxFromBracket(annualTaxableIncome, bracket);
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