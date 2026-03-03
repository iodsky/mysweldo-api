package com.iodsky.mysweldo.payroll.tax;

import org.springframework.stereotype.Component;

@Component
public class IncomeTaxBracketMapper {

    public IncomeTaxBracketDto toDto(IncomeTaxBracket entity) {
        if (entity == null) {
            return null;
        }

        return IncomeTaxBracketDto.builder()
                .id(entity.getId())
                .minIncome(entity.getMinIncome())
                .maxIncome(entity.getMaxIncome())
                .baseTax(entity.getBaseTax())
                .marginalRate(entity.getMarginalRate())
                .threshold(entity.getThreshold())
                .effectiveDate(entity.getEffectiveDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
