package com.iodsky.mysweldo.payroll.contribution.pagIbig;

import org.springframework.stereotype.Component;

@Component
public class PagibigRateTableMapper {

    public PagibigRateTableDto toDto(PagibigRateTable entity) {
        if (entity == null) {
            return null;
        }

        return PagibigRateTableDto.builder()
                .id(entity.getId())
                .employeeRate(entity.getEmployeeRate())
                .employerRate(entity.getEmployerRate())
                .lowIncomeThreshold(entity.getLowIncomeThreshold())
                .lowIncomeEmployeeRate(entity.getLowIncomeEmployeeRate())
                .maxSalaryCap(entity.getMaxSalaryCap())
                .effectiveDate(entity.getEffectiveDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
