package com.iodsky.mysweldo.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBasicDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private DepartmentBasicDto department;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PositionBasicDto position;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private EmploymentStatus status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private EmploymentType type;
}
