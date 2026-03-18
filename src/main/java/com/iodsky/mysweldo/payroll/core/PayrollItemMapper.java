package com.iodsky.mysweldo.payroll.core;

import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class PayrollItemMapper {

    public PayrollItemDto toDto(PayrollItem payroll) {
        if (payroll == null) return null;

        return PayrollItemDto.builder()
                .id(payroll.getId())
                .employeeId(payroll.getEmployee() != null ? payroll.getEmployee().getId() : null)
                .periodStartDate(payroll.getPayrollRun() != null ? payroll.getPayrollRun().getPeriodStartDate() : null)
                .periodEndDate(payroll.getPayrollRun() != null ? payroll.getPayrollRun().getPeriodEndDate() : null)

                // Work & time
                .daysWorked(payroll.getDaysWorked())
                .absences(payroll.getAbsences())
                .tardinessMinutes(payroll.getTardinessMinutes())
                .undertimeMinutes(payroll.getUndertimeMinutes())
                .overtimeMinutes(payroll.getOvertimeMinutes())
                .overtimePay(payroll.getOvertimePay())

                // Rates
                .monthlyRate(payroll.getMonthlyRate())
                .semiMonthlyRate(payroll.getSemiMonthlyRate())
                .dailyRate(payroll.getDailyRate())
                .hourlyRate(payroll.getHourlyRate())
                .salaryType(payroll.getSalaryType())

                // Payroll amounts
                .totalBenefits(payroll.getTotalBenefits())
                .grossPay(payroll.getGrossPay())
                .totalDeductions(payroll.getTotalDeductions())
                .netPay(payroll.getNetPay())

                // Related entities
                .benefits(payroll.getBenefits() != null
                        ? payroll.getBenefits().stream().map(this::toDto).toList()
                        : Collections.emptyList()
                )
                .deductions(payroll.getDeductions() != null
                        ? payroll.getDeductions().stream().map(this::toDto).toList()
                        : Collections.emptyList()
                )
                .build();
    }

    private PayrollBenefitDto toDto(PayrollBenefit entity) {
        if (entity == null) return null;

        return PayrollBenefitDto.builder()
                .benefit(entity.getBenefit().getDescription())
                .amount(entity.getAmount())
                .build();
    }

    private PayrollDeductionDto toDto(PayrollDeduction entity) {
        if (entity == null) return null;

        return PayrollDeductionDto.builder()
                .deduction(entity.getDeduction().getCode())
                .amount(entity.getAmount())
                .build();
    }


}
