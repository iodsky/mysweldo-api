package com.iodsky.mysweldo.benefit;

import org.springframework.stereotype.Component;

@Component
public class BenefitMapper {

    public BenefitDto toDto(Benefit benefit) {
        if (benefit == null) {
            return null;
        }

        return BenefitDto.builder()
                .benefit(benefit.getBenefitType().getType())
                .amount(benefit.getAmount())
                .build();
    }

    public Benefit toEntity(BenefitRequest request) {
        if (request == null) {
            return  null;
        }

        return Benefit.builder()
                .benefitType(BenefitType.builder().id(request.getBenefitTypeId()).build())
                .amount(request.getAmount())
                .build();
    }

}
