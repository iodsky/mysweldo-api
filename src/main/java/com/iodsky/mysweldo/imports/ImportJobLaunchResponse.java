package com.iodsky.mysweldo.imports;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportJobLaunchResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID importJobId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}