package com.iodsky.mysweldo.tax;

import org.springframework.stereotype.Component;

@Component
public class TaxBracketMapper {

    public TaxBracketDto toDto(TaxBracket entity) {
        if (entity == null) {
            return null;
        }

        return TaxBracketDto.builder()
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
