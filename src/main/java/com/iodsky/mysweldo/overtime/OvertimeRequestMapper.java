package com.iodsky.mysweldo.overtime;

import org.springframework.stereotype.Component;

@Component
public class OvertimeRequestMapper {

    public OvertimeRequestDto toDto(OvertimeRequest entity) {
        if (entity == null) return null;

        return OvertimeRequestDto.builder()
                .id(entity.getId())
                .employeeId(entity.getEmployee().getId())
                .date(entity.getDate())
                .overtimeHours(entity.getOvertimeHours())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy() == null ? null : entity.getCreatedBy().getId())
                .lastModifiedBy(entity.getLastModifiedBy() == null ? null : entity.getLastModifiedBy().getId())
                .build();
    }

}
