package com.iodsky.mysweldo.batch.response;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.core.BatchStatus;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDetailsResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long jobExecutionId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BatchStatus status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime endTime;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long readCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long writeCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skipCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String exitDescription;
}
