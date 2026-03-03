package com.iodsky.mysweldo.benefit;

import org.springframework.stereotype.Component;

@Component
public class BenefitTypeMapper {

    public BenefitTypeDto toDto(BenefitType entity) {
        if (entity == null) {
            return null;
        }

        return BenefitTypeDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
