package com.iodsky.mysweldo.sss;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SssRateMapper {

    public SssRateDto toDto(SssRate entity) {
        if (entity == null) {
            return null;
        }

        return SssRateDto.builder()
                .id(entity.getId())
                .totalSss(entity.getTotalSss())
                .employeeSss(entity.getEmployeeRate())
                .employerSss(entity.getEmployerRate())
                .salaryBrackets(entity.getSalaryBrackets().stream()
                        .map(this::toBracketDto)
                        .collect(Collectors.toList()))
                .effectiveDate(entity.getEffectiveDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private SssRateDto.SalaryBracketDto toBracketDto(SssRate.SalaryBracket bracket) {
        return SssRateDto.SalaryBracketDto.builder()
                .minSalary(bracket.getMinSalary())
                .maxSalary(bracket.getMaxSalary())
                .msc(bracket.getMsc())
                .build();
    }
}
