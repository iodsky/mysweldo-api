package com.iodsky.mysweldo.deduction;

import org.springframework.stereotype.Component;

@Component
public class DeductionMapper {

    public DeductionDto toDto(Deduction entity) {
        if (entity == null) {
            return null;
        }

        return DeductionDto.builder()
                .code(entity.getCode())
                .description(entity.getDescription())
                .statutory(entity.isStatutory())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
