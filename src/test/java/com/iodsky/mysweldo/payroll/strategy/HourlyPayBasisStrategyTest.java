package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class HourlyPayBasisStrategyTest {

    private final HourlyPayBasisStrategy strategy =
            new HourlyPayBasisStrategy(new PayrollCalculator(null, null, null, null));

    private AttendancePayrollSummary attendance(double daysWorked, double absenceDays, int tardy, int undertime) {
        return AttendancePayrollSummary.builder()
                .daysWorked(BigDecimal.valueOf(daysWorked))
                .absenceDays(BigDecimal.valueOf(absenceDays))
                .tardinessMinutes(tardy)
                .undertimeMinutes(undertime)
                .build();
    }

    @Test
    void compute_paysRegularHoursWorked() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(150), attendance(10, 0, 0, 0), BigDecimal.valueOf(80));

        assertThat(result.regularPay()).isEqualByComparingTo("12000.00");
        assertThat(result.dailyRate()).isEqualByComparingTo("1200.00");
        assertThat(result.hourlyRate()).isEqualByComparingTo("150");
        assertThat(result.monthlyEquivalent()).isEqualByComparingTo("26100.00");
        assertThat(result.semiMonthlyRate()).isEqualByComparingTo("13050.00");
    }

    @Test
    void compute_neverAppliesAttendanceDeductions() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(150), attendance(8, 2, 45, 30), BigDecimal.valueOf(60));

        assertThat(result.absenceDeduction()).isEqualByComparingTo("0");
        assertThat(result.tardinessDeduction()).isEqualByComparingTo("0");
        assertThat(result.undertimeDeduction()).isEqualByComparingTo("0");
        assertThat(result.regularPay()).isEqualByComparingTo("9000.00");
    }

    @Test
    void compute_zeroRegularHoursYieldsZeroPay() {
        PayBasisResult result = strategy.compute(
                BigDecimal.valueOf(150), attendance(0, 0, 0, 0), BigDecimal.ZERO);

        assertThat(result.regularPay()).isEqualByComparingTo("0.00");
    }
}
