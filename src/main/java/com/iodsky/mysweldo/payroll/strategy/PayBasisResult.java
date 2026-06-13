package com.iodsky.mysweldo.payroll.strategy;

import java.math.BigDecimal;

/**
 * Result of a pay-basis computation: the rate ladder, attendance-based
 * deductions, and the period base pay for one employee.
 *
 * Field semantics per basis:
 * <ul>
 *   <li>MONTHLY — monthlyEquivalent is the salary rate; regularPay is the
 *       semi-monthly rate minus absence/tardiness/undertime deductions.</li>
 *   <li>DAILY — monthlyEquivalent is dailyRate × 21.75; regularPay is
 *       dailyRate × daysWorked minus tardiness/undertime; absence is never
 *       deducted (unworked days are simply unpaid).</li>
 *   <li>HOURLY — monthlyEquivalent is hourlyRate × 8 × 21.75; regularPay is
 *       hourlyRate × regular hours worked; no attendance deductions apply
 *       (lateness is already reflected in hours).</li>
 * </ul>
 * For DAILY/HOURLY, semiMonthlyRate is notional (monthlyEquivalent / 2), not
 * the earned base pay. Deduction fields are always non-null (ZERO when the
 * basis does not apply them).
 *
 * @param monthlyEquivalent salary base used for SSS/PhilHealth/Pag-IBIG/tax
 * @param semiMonthlyRate   monthlyEquivalent / 2
 * @param dailyRate         daily rate for this basis
 * @param hourlyRate        hourly rate for this basis
 * @param absenceDeduction  ZERO for DAILY/HOURLY
 * @param tardinessDeduction ZERO for HOURLY
 * @param undertimeDeduction ZERO for HOURLY
 * @param regularPay        period base pay net of the above deductions
 */
public record PayBasisResult(
        BigDecimal monthlyEquivalent,
        BigDecimal semiMonthlyRate,
        BigDecimal dailyRate,
        BigDecimal hourlyRate,
        BigDecimal absenceDeduction,
        BigDecimal tardinessDeduction,
        BigDecimal undertimeDeduction,
        BigDecimal regularPay
) {
}
