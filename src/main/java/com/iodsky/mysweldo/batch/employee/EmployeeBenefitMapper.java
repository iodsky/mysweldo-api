package com.iodsky.mysweldo.batch.employee;

import com.iodsky.mysweldo.benefit.Benefit;
import org.springframework.stereotype.Component;

@Component
public class EmployeeBenefitMapper {

    public EmployeeBenefitDto toDto(EmployeeBenefit benefit) {
        if (benefit == null) {
            return null;
        }

        return EmployeeBenefitDto.builder()
                .benefit(benefit.getBenefit().getDescription())
                .amount(benefit.getAmount())
                .build();
    }

    public EmployeeBenefit toEntity(EmployeeBenefitRequest request) {
        if (request == null) {
            return  null;
        }

        return EmployeeBenefit.builder()
                .benefit(Benefit.builder().code(request.getBenefitCode()).build())
                .amount(request.getAmount())
                .build();
    }

}
