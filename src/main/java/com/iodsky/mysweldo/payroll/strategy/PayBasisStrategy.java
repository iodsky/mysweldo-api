package com.iodsky.mysweldo.payroll.strategy;

import com.iodsky.mysweldo.attendance.AttendancePayrollSummary;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;

import java.math.BigDecimal;

/**
 * Computes the period base pay and rate ladder for one pay basis (PayType).
 * The basis determines how Salary.rate is interpreted (monthly, daily, or
 * hourly) and which attendance-based deductions apply.
 */
public interface PayBasisStrategy {
    /**
     * Computes the pay-basis result for an employee for one payroll period.
     *
     * @param rate         Salary.rate, interpreted per the implementing basis
     * @param attendance   attendance summary for the period
     * @param regularHours non-overtime hours worked, already capped at
     *                     daysWorked × 8 and floored at zero; only the HOURLY
     *                     basis uses it
     * @param frequency    payroll frequency, used to derive the period base rate
     * @return the computed rates, deductions, and period base pay
     */
    PayBasisResult compute(BigDecimal rate, AttendancePayrollSummary attendance, BigDecimal regularHours, PayrollFrequency frequency);
}
