package com.iodsky.mysweldo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportJobErrorDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long rowNumber;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;
}