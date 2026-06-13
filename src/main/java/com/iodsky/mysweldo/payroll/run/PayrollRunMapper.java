package com.iodsky.mysweldo.payroll.run;

import org.springframework.stereotype.Component;

@Component
public class PayrollRunMapper {

    public PayrollRunDto toDto(PayrollRun entity) {
        if (entity == null) return null;

        return PayrollRunDto.builder()
                .id(entity.getId())
                .periodStartDate(entity.getPeriod().getStartDate())
                .periodEndDate(entity.getPeriod().getEndDate())
                .payrollFrequency(entity.getPeriod().getFrequency())
                .type(entity.getType())
                .status(entity.getStatus())
                .totalGrossPay(entity.getTotalGrossPay())
                .totalBenefits(entity.getTotalBenefits())
                .totalDeductions(entity.getTotalDeductions())
                .totalNetPay(entity.getTotalNetPay())
                .totalEmployerCost(entity.getTotalEmployerCost())
                .build();
    }

}
