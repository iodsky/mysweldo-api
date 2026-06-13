package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPayBasisStrategyTest {

    private final DailyPayBasisStrategy strategy =
            new DailyPayBasisStrategy(new PayrollCalculator(null, null, null, null));

    private AttendancePayrollSummary attendance(double daysWorked, double absenceDays, int tardy, int undertime) {
        return AttendancePayrollSummary.builder()
                .daysWorked(BigDecimal.valueOf(daysWorked))
                .absenceDays(BigDecimal.valueOf(absenceDays))
                .tardinessMinutes(tardy)
                .undertimeMinutes(undertime)
                .build();
    }

    @Test
    void compute_paysDaysWorkedAndNeverDeductsAbsences() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(800), attendance(10, 3, 0, 0), BigDecimal.valueOf(80));

        assertThat(result.regularPay()).isEqualByComparingTo("8000.00");
        assertThat(result.absenceDeduction()).isEqualByComparingTo("0");
        assertThat(result.monthlyEquivalent()).isEqualByComparingTo("17400.00");
        assertThat(result.semiMonthlyRate()).isEqualByComparingTo("8700.00");
        assertThat(result.dailyRate()).isEqualByComparingTo("800");
        assertThat(result.hourlyRate()).isEqualByComparingTo("100.00");
    }

    @Test
    void compute_deductsTardinessAndUndertimeFromEarnedPay() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(800), attendance(10, 0, 30, 0), BigDecimal.valueOf(80));

        assertThat(result.tardinessDeduction()).isEqualByComparingTo("50.00");
        assertThat(result.regularPay()).isEqualByComparingTo("7950.00");
    }

    @Test
    void compute_zeroDaysWorkedYieldsZeroPay() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(800), attendance(0, 10, 0, 0), BigDecimal.ZERO);

        assertThat(result.regularPay()).isEqualByComparingTo("0.00");
    }
}
