package com.iodsky.mysweldo.payroll.core;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PayrollItemMapper {

    public PayrollItemDto toDto(PayrollItem payroll) {
        if (payroll == null) return null;

        return PayrollItemDto.builder()
                .id(payroll.getId())
                .employeeId(payroll.getEmployee().getId())
                .periodStartDate(payroll.getPayrollRun().getPeriodStartDate())
                .periodEndDate(payroll.getPayrollRun().getPeriodEndDate())
                .daysWorked(payroll.getDaysWorked())
                .overtime(payroll.getOvertime())
                .monthlyRate(payroll.getMonthlyRate())
                .dailyRate(payroll.getDailyRate())
                .grossPay(payroll.getGrossPay())
                .benefits(payroll.getBenefits()
                        .stream()
                        .map(this::toDto)
                        .toList()
                )
                .deductions(payroll.getDeductions()
                        .stream()
                        .map(this::toDto)
                        .toList()
                )
                .employerContributions(payroll.getEmployerContributions()
                        .stream()
                        .map(this::toDto)
                        .toList()
                )
                .netPay(payroll.getNetPay())
                .build();
    }

    private BigDecimal getDeductionAmount(PayrollItem payroll, String type) {
        return payroll.getDeductions().stream()
                .filter(d -> d.getDeduction().getCode().equalsIgnoreCase(type))
                .map(PayrollDeduction::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getBenefitAmount(PayrollItem payroll, String type) {
        return payroll.getBenefits().stream()
                .filter(b -> b.getBenefit().getCode().equalsIgnoreCase(type))
                .map(PayrollBenefit::getAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO);
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

    private EmployerContributionDto toDto(EmployerContribution entity) {
        if (entity == null) return null;

        return EmployerContributionDto.builder()
                .contribution(entity.getContribution().getCode())
                .amount(entity.getAmount())
                .build();
    }

}
