package com.iodsky.mysweldo.department;

import org.springframework.stereotype.Component;

@Component
public class DepartmentMapper {

    public DepartmentDto toDto(Department department) {
        return DepartmentDto.builder()
                .id(department.getId())
                .title(department.getTitle())
                .createdAt(department.getCreatedAt())
                .updatedAt(department.getUpdatedAt())
                .build();
    }

    public Department toEntity(DepartmentRequest request) {
        return Department.builder()
                .id(request.getId())
                .title(request.getTitle())
                .build();
    }
}
