package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pay basis for DAILY-rated employees. The salary rate is a per-day amount:
 * period base pay is dailyRate × daysWorked, reduced by tardiness and
 * undertime deductions. Absences are never deducted — unworked days are
 * simply unpaid. Statutory contributions and tax use a standardized monthly
 * equivalent of dailyRate × 21.75.
 */
@Component
@RequiredArgsConstructor
public class DailyPayBasisStrategy implements PayBasisStrategy {

    private final PayrollCalculator payrollCalculator;

    @Override
    public PayBasisResult compute(BigDecimal rate, AttendancePayrollSummary attendance, BigDecimal regularHours) {
        BigDecimal hourlyRate = payrollCalculator.calculateHourlyRate(rate);
        BigDecimal monthlyEquivalent = payrollCalculator.calculateMonthlyEquivalentFromDailyRate(rate);
        BigDecimal semiMonthlyRate = payrollCalculator.calculateSemiMonthlyRate(monthlyEquivalent);

        BigDecimal periodBasePay = payrollCalculator.calculateDailyBasisPay(rate, attendance.getDaysWorked());

        BigDecimal tardinessDeduction = payrollCalculator.calculateTardinessDeduction(
                hourlyRate,
                attendance.getTardinessMinutes()
        );

        BigDecimal undertimeDeduction = payrollCalculator.calculateUndertimeDeduction(
                hourlyRate,
                attendance.getUndertimeMinutes()
        );

        BigDecimal regularPay = payrollCalculator.calculateRegularPay(
                periodBasePay,
                BigDecimal.ZERO,
                tardinessDeduction,
                undertimeDeduction
        );

        return new PayBasisResult(
                monthlyEquivalent,
                semiMonthlyRate,
                rate,
                hourlyRate,
                BigDecimal.ZERO,
                tardinessDeduction,
                undertimeDeduction,
                regularPay
        );
    }
}
