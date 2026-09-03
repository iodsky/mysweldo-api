package com.iodsky.mysweldo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportJobSummaryDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID importJobId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ImportType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ImportStatus status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long readCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long writeCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private long skipCount;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startedAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant finishedAt;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String errorMessage;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
}