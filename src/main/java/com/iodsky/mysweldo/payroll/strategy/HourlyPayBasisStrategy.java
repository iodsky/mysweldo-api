package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.core.PayrollCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pay basis for HOURLY-rated employees. The salary rate is a per-hour amount:
 * period base pay is hourlyRate × regular hours worked. Hours beyond
 * daysWorked × 8 without an approved overtime request are unpaid. No
 * attendance deductions apply — lateness and undertime are already reflected
 * in the hours worked. Statutory contributions and tax use a standardized
 * monthly equivalent of hourlyRate × 8 × 21.75.
 */
@Component
@RequiredArgsConstructor
public class HourlyPayBasisStrategy implements PayBasisStrategy {

    private final PayrollCalculator payrollCalculator;

    @Override
    public PayBasisResult compute(BigDecimal rate, AttendancePayrollSummary attendance, BigDecimal regularHours) {
        BigDecimal dailyRate = payrollCalculator.calculateDailyRateFromHourlyRate(rate);
        BigDecimal monthlyEquivalent = payrollCalculator.calculateMonthlyEquivalentFromDailyRate(dailyRate);
        BigDecimal semiMonthlyRate = payrollCalculator.calculateSemiMonthlyRate(monthlyEquivalent);

        BigDecimal regularPay = payrollCalculator.calculateHourlyBasisPay(rate, regularHours);

        return new PayBasisResult(
                monthlyEquivalent,
                semiMonthlyRate,
                dailyRate,
                rate,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                regularPay
        );
    }
}
