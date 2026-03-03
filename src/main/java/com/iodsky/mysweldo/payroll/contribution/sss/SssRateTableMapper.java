package com.iodsky.mysweldo.payroll.contribution.sss;

import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class SssRateTableMapper {

    public SssRateTableDto toDto(SssRateTable entity) {
        if (entity == null) {
            return null;
        }

        return SssRateTableDto.builder()
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

    private SssRateTableDto.SalaryBracketDto toBracketDto(SssRateTable.SalaryBracket bracket) {
        return SssRateTableDto.SalaryBracketDto.builder()
                .minSalary(bracket.getMinSalary())
                .maxSalary(bracket.getMaxSalary())
                .msc(bracket.getMsc())
                .build();
    }
}
