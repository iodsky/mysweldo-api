package com.iodsky.mysweldo.pagIbig;

import org.springframework.stereotype.Component;

@Component
public class PagibigRateMapper {

    public PagibigRateDto toDto(PagibigRate entity) {
        if (entity == null) {
            return null;
        }

        return PagibigRateDto.builder()
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
