package com.iodsky.mysweldo.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ReportType type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private ReportFormat format;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private UUID payrollRunId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;

}