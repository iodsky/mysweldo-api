package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.sss.SssRate;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculatorTest {

    private final PayrollCalculator calculator = new PayrollCalculator(null, null, null, null);

    private SssRate sssRate() {
        return SssRate.builder()
                .employeeRate(new BigDecimal("0.045"))
                .employerRate(new BigDecimal("0.095"))
                .salaryBrackets(List.of(
                        new SssRate.SalaryBracket(BigDecimal.ZERO, BigDecimal.valueOf(20000), BigDecimal.valueOf(10000)),
                        new SssRate.SalaryBracket(new BigDecimal("20000.01"), null, BigDecimal.valueOf(20000))
                ))
                .build();
    }

    private PhilhealthRate philhealthRate() {
        return PhilhealthRate.builder()
                .premiumRate(new BigDecimal("0.05"))
                .minSalaryFloor(BigDecimal.valueOf(10000))
                .maxSalaryCap(BigDecimal.valueOf(100000))
                .fixedContribution(BigDecimal.valueOf(500))
                .build();
    }

    private PagibigRate pagibigRate() {
        return PagibigRate.builder()
                .employeeRate(new BigDecimal("0.02"))
                .employerRate(new BigDecimal("0.02"))
                .lowIncomeThreshold(BigDecimal.valueOf(1500))
                .lowIncomeEmployeeRate(new BigDecimal("0.01"))
                .maxSalaryCap(BigDecimal.valueOf(10000))
                .build();
    }

    @Test
    void calculateMonthlyEquivalentFromDailyRate_multipliesByAverageWorkingDays() {
        BigDecimal result = calculator.calculateMonthlyEquivalentFromDailyRate(BigDecimal.valueOf(800));

        assertThat(result).isEqualByComparingTo("17400.00");
    }

    @Test
    void calculateMonthlyEquivalentFromDailyRate_roundsHalfUp() {
        BigDecimal result = calculator.calculateMonthlyEquivalentFromDailyRate(new BigDecimal("800.50"));

        // 800.50 * 21.75 = 17410.875 -> 17410.88
        assertThat(result).isEqualByComparingTo("17410.88");
    }

    @Test
    void calculateDailyRateFromHourlyRate_multipliesByStandardWorkHours() {
        BigDecimal result = calculator.calculateDailyRateFromHourlyRate(BigDecimal.valueOf(150));

        assertThat(result).isEqualByComparingTo("1200.00");
    }

    @Test
    void calculateDailyBasisPay_multipliesRateByDaysWorked() {
        BigDecimal result = calculator.calculateDailyBasisPay(BigDecimal.valueOf(800), BigDecimal.TEN);

        assertThat(result).isEqualByComparingTo("8000.00");
    }

    @Test
    void calculateDailyBasisPay_zeroDaysWorkedYieldsZero() {
        BigDecimal result = calculator.calculateDailyBasisPay(BigDecimal.valueOf(800), BigDecimal.ZERO);

        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateHourlyBasisPay_multipliesRateByRegularHours() {
        BigDecimal result = calculator.calculateHourlyBasisPay(BigDecimal.valueOf(150), BigDecimal.valueOf(80));

        assertThat(result).isEqualByComparingTo("12000.00");
    }

    @Test
    void calculateHourlyBasisPay_clampsNegativeToZero() {
        BigDecimal result = calculator.calculateHourlyBasisPay(BigDecimal.valueOf(150), BigDecimal.valueOf(-8));

        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateSssDeduction_semiMonthly_halvesMonthlyContribution() {
        // MSC 10000 * 0.045 / 2 = 225.00
        BigDecimal result = calculator.calculateSssDeduction(
                BigDecimal.valueOf(20000), sssRate(), PayrollFrequency.SEMI_MONTHLY);

        assertThat(result).isEqualByComparingTo("225.00");
    }

    @Test
    void calculateSssDeduction_monthly_returnsFullMonthlyContribution() {
        // MSC 10000 * 0.045 / 1 = 450.00
        BigDecimal result = calculator.calculateSssDeduction(
                BigDecimal.valueOf(20000), sssRate(), PayrollFrequency.MONTHLY);

        assertThat(result).isEqualByComparingTo("450.00");
    }

    @Test
    void calculatePhilhealthDeduction_semiMonthly_quartersMonthlyPremium() {
        // 20000 * 0.05 / 2 (ee share) / 2 (period) = 250.00
        BigDecimal result = calculator.calculatePhilhealthDeduction(
                BigDecimal.valueOf(20000), philhealthRate(), PayrollFrequency.SEMI_MONTHLY);

        assertThat(result).isEqualByComparingTo("250.00");
    }

    @Test
    void calculatePhilhealthDeduction_monthly_halvesMonthlyPremiumForEmployeeShare() {
        // 20000 * 0.05 / 2 (ee share) / 1 (period) = 500.00
        BigDecimal result = calculator.calculatePhilhealthDeduction(
                BigDecimal.valueOf(20000), philhealthRate(), PayrollFrequency.MONTHLY);

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculatePagibigDeduction_semiMonthly_halvesMonthlyContribution() {
        // min(20000, 10000) * 0.02 / 2 = 100.00
        BigDecimal result = calculator.calculatePagibigDeduction(
                BigDecimal.valueOf(20000), pagibigRate(), PayrollFrequency.SEMI_MONTHLY);

        assertThat(result).isEqualByComparingTo("100.00");
    }

    @Test
    void calculatePagibigDeduction_monthly_returnsFullMonthlyContribution() {
        // min(20000, 10000) * 0.02 / 1 = 200.00
        BigDecimal result = calculator.calculatePagibigDeduction(
                BigDecimal.valueOf(20000), pagibigRate(), PayrollFrequency.MONTHLY);

        assertThat(result).isEqualByComparingTo("200.00");
    }
}
