package com.iodsky.mysweldo.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentBasicDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
}
