package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.calc.PayrollCalculator;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Pay basis for MONTHLY-salaried employees. The salary rate is a monthly
 * amount: period base pay is monthlyRate / periodsPerMonth(frequency), reduced
 * by absence, tardiness, and undertime deductions.
 */
@Component
@RequiredArgsConstructor
public class MonthlyPayBasisStrategy implements PayBasisStrategy {

    private final PayrollCalculator payrollCalculator;

    @Override
    public PayBasisResult compute(BigDecimal rate, AttendancePayrollSummary attendance, BigDecimal regularHours, PayrollFrequency frequency) {
        BigDecimal periodRate = payrollCalculator.calculatePeriodRate(rate, frequency);
        BigDecimal dailyRate = payrollCalculator.calculateDailyRate(rate);
        BigDecimal hourlyRate = payrollCalculator.calculateHourlyRate(dailyRate);

        BigDecimal absenceDeduction = payrollCalculator.calculateAbsenceDeduction(
                dailyRate,
                attendance.getAbsenceDays()
        );

        BigDecimal tardinessDeduction = payrollCalculator.calculateTardinessDeduction(
                hourlyRate,
                attendance.getTardinessMinutes()
        );

        BigDecimal undertimeDeduction = payrollCalculator.calculateUndertimeDeduction(
                hourlyRate,
                attendance.getUndertimeMinutes()
        );

        BigDecimal regularPay = payrollCalculator.calculateRegularPay(
                periodRate,
                absenceDeduction,
                tardinessDeduction,
                undertimeDeduction
        );

        return new PayBasisResult(
                rate,
                periodRate,
                dailyRate,
                hourlyRate,
                absenceDeduction,
                tardinessDeduction,
                undertimeDeduction,
                regularPay
        );
    }
}
