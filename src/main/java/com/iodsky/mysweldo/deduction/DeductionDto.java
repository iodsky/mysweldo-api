package com.iodsky.mysweldo.deduction;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeductionDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean statutory;
    private Instant createdAt;
    private Instant updatedAt;
}
