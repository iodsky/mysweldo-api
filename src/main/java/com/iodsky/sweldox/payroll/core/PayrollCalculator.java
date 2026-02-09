package com.iodsky.sweldox.payroll.core;

import com.iodsky.sweldox.attendance.Attendance;
import com.iodsky.sweldox.benefit.Benefit;
import com.iodsky.sweldox.payroll.contribution.pagIbig.PagibigContribution;
import com.iodsky.sweldox.payroll.contribution.pagIbig.PagibigContributionRepository;
import com.iodsky.sweldox.payroll.contribution.philhealth.PhilhealthContribution;
import com.iodsky.sweldox.payroll.contribution.philhealth.PhilhealthContributionRepository;
import com.iodsky.sweldox.payroll.contribution.sss.SssContribution;
import com.iodsky.sweldox.payroll.contribution.sss.SssContributionRepository;
import com.iodsky.sweldox.payroll.tax.IncomeTaxBracket;
import com.iodsky.sweldox.payroll.tax.IncomeTaxBracketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PayrollCalculator {

    private final PhilhealthContributionRepository philhealthContributionRepository;
    private final PagibigContributionRepository pagibigContributionRepository;
    private final SssContributionRepository sssContributionRepository;
    private final IncomeTaxBracketRepository incomeTaxBracketRepository;

    private static final BigDecimal SEMI_MONTHLY_DIVISOR = BigDecimal.valueOf(2);
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(1.25);
    private static final int STANDARD_WORK_HOURS = 8;

    public PayrollConfiguration loadConfiguration(LocalDate payrollDate) {
        PhilhealthContribution philhealth = philhealthContributionRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PhilHealth contribution configuration not found for date: " + payrollDate
                ));

        PagibigContribution pagibig = pagibigContributionRepository
                .findLatestByEffectiveDate(payrollDate)
            .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pag-IBIG contribution configuration not found for date: " + payrollDate
                ));

        SssContribution sssContribution = sssContributionRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SSS contribution configuration not found for date: " + payrollDate
                ));

        List<IncomeTaxBracket> taxBrackets = incomeTaxBracketRepository
                .findAllByEffectiveDate(payrollDate);

        if (taxBrackets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Income tax bracket configurations not found for date: " + payrollDate
            );
        }

        return PayrollConfiguration.builder()
                .philhealthContribution(philhealth)
                .pagibigContribution(pagibig)
                .sssContribution(sssContribution)
                .incomeTaxBrackets(taxBrackets)
                .build();
    }

    public BigDecimal calculateTotalHours(List<Attendance> attendances) {
        return attendances.stream()
                .map(Attendance::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateOvertimeHours(List<Attendance> attendances) {
        return attendances.stream()
                .map(Attendance::getOvertime)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateDailyRate(BigDecimal hourlyRate) {
        return hourlyRate.multiply(BigDecimal.valueOf(STANDARD_WORK_HOURS));
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
        PhilhealthContribution config = philhealthContributionRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "PhilHealth contribution configuration not found for date: " + payrollDate
                ));

        return calculatePhilhealthDeduction(basicSalary, config);
    }

    public BigDecimal calculatePhilhealthDeduction(BigDecimal basicSalary, PhilhealthContribution config) {
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
        PagibigContribution config = pagibigContributionRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Pag-IBIG contribution configuration not found for date: " + payrollDate
                ));

        return calculatePagibigDeduction(basicSalary, config);
    }

    public BigDecimal calculatePagibigDeduction(BigDecimal basicSalary, PagibigContribution config) {
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
        SssContribution config = sssContributionRepository
                .findLatestByEffectiveDate(payrollDate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "SSS contribution configuration not found for date: " + payrollDate
                ));

        return calculateSssDeduction(basicSalary, config);
    }

    public BigDecimal calculateSssDeduction(BigDecimal basicSalary, SssContribution sssContribution) {
        // Find the appropriate salary bracket
        SssContribution.SalaryBracket bracket = sssContribution.findBracket(basicSalary);

        // Calculate the monthly contribution based on MSC
        BigDecimal monthlyContribution = bracket.getMsc().multiply(sssContribution.getEmployeeRate());

        // Divide by 2 for semi-monthly payroll
        return monthlyContribution.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateWithholdingTax(BigDecimal taxableIncome, LocalDate payrollDate) {
        IncomeTaxBracket bracket = incomeTaxBracketRepository
                .findByIncomeAndEffectiveDate(taxableIncome, payrollDate);

        if (bracket == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Income tax bracket not found for income: " + taxableIncome + " and date: " + payrollDate
            );
        }

        return calculateWithholdingTaxFromBracket(taxableIncome, bracket);
    }


    public BigDecimal calculateWithholdingTax(BigDecimal taxableIncome, List<IncomeTaxBracket> taxBrackets) {
        IncomeTaxBracket bracket = taxBrackets.stream()
                .filter(b -> taxableIncome.compareTo(b.getMinIncome()) >= 0
                        && (b.getMaxIncome() == null || taxableIncome.compareTo(b.getMaxIncome()) <= 0))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Income tax bracket not found for income: " + taxableIncome
                ));

        return calculateWithholdingTaxFromBracket(taxableIncome, bracket);
    }


    private BigDecimal calculateWithholdingTaxFromBracket(BigDecimal taxableIncome, IncomeTaxBracket bracket) {
        BigDecimal excessAmount = taxableIncome.subtract(bracket.getThreshold()).max(BigDecimal.ZERO);
        BigDecimal excessTax = excessAmount.multiply(bracket.getMarginalRate());
        BigDecimal monthlyTax = bracket.getBaseTax().add(excessTax);

        return monthlyTax.divide(SEMI_MONTHLY_DIVISOR, 2, RoundingMode.HALF_UP);
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