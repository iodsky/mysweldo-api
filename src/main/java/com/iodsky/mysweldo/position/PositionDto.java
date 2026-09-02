package com.iodsky.mysweldo.position;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PositionDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String departmentId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String departmentTitle;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}
