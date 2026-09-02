package com.iodsky.mysweldo.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobLaunchResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long jobExecutionId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;
}
