package com.iodsky.mysweldo.payroll.core;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollCalculatorTest {

    private final PayrollCalculator calculator = new PayrollCalculator(null, null, null, null);

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
}
