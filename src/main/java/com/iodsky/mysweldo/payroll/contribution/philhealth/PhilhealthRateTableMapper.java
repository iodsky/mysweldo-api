package com.iodsky.mysweldo.payroll.contribution.philhealth;

import org.springframework.stereotype.Component;

@Component
public class PhilhealthRateTableMapper {

    public PhilhealthRateTableDto toDto(PhilhealthRateTable entity) {
        if (entity == null) {
            return null;
        }

        return PhilhealthRateTableDto.builder()
                .id(entity.getId())
                .premiumRate(entity.getPremiumRate())
                .maxSalaryCap(entity.getMaxSalaryCap())
                .minSalaryFloor(entity.getMinSalaryFloor())
                .fixedContribution(entity.getFixedContribution())
                .effectiveDate(entity.getEffectiveDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
