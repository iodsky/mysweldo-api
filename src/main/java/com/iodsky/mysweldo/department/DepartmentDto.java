package com.iodsky.mysweldo.department;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DepartmentDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}
