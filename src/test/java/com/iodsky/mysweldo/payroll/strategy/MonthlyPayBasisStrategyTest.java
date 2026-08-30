package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.calc.PayrollCalculator;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MonthlyPayBasisStrategyTest {

    private final MonthlyPayBasisStrategy strategy =
            new MonthlyPayBasisStrategy(new PayrollCalculator());

    private AttendancePayrollSummary attendance(double daysWorked, double absenceDays, int tardy, int undertime) {
        return AttendancePayrollSummary.builder()
                .daysWorked(BigDecimal.valueOf(daysWorked))
                .absenceDays(BigDecimal.valueOf(absenceDays))
                .tardinessMinutes(tardy)
                .undertimeMinutes(undertime)
                .build();
    }

    @Test
    void compute_semiMonthly_derivesHalfMonthlyRateAsBase() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(20000), attendance(10, 0, 0, 0), BigDecimal.valueOf(80), PayrollFrequency.SEMI_MONTHLY);

        assertThat(result.monthlyEquivalent()).isEqualByComparingTo("20000");
        assertThat(result.semiMonthlyRate()).isEqualByComparingTo("10000.00");
        assertThat(result.dailyRate()).isEqualByComparingTo("919.54");
        assertThat(result.hourlyRate()).isEqualByComparingTo("114.94");
        assertThat(result.regularPay()).isEqualByComparingTo("10000.00");
    }

    @Test
    void compute_monthly_usesFullMonthlyRateAsBase() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(20000), attendance(10, 0, 0, 0), BigDecimal.valueOf(80), PayrollFrequency.MONTHLY);

        assertThat(result.monthlyEquivalent()).isEqualByComparingTo("20000");
        assertThat(result.semiMonthlyRate()).isEqualByComparingTo("20000.00");
        assertThat(result.regularPay()).isEqualByComparingTo("20000.00");
    }

    @Test
    void compute_semiMonthly_appliesAbsenceTardinessAndUndertimeDeductions() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(20000), attendance(9, 1, 30, 15), BigDecimal.valueOf(72), PayrollFrequency.SEMI_MONTHLY);

        assertThat(result.absenceDeduction()).isEqualByComparingTo("919.54");
        assertThat(result.tardinessDeduction()).isEqualByComparingTo("57.47");
        assertThat(result.undertimeDeduction()).isEqualByComparingTo("28.74");
        // 10000 - 919.54 - 57.47 - 28.74
        assertThat(result.regularPay()).isEqualByComparingTo("8994.25");
    }

    @Test
    void compute_clampsRegularPayAtZeroForExtremeAbsences() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(20000), attendance(0, 30, 0, 0), BigDecimal.ZERO, PayrollFrequency.SEMI_MONTHLY);

        assertThat(result.regularPay()).isEqualByComparingTo("0.00");
    }
}
