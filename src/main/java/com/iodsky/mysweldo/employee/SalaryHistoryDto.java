package com.iodsky.mysweldo.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class SalaryHistoryDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal rate;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayType payType;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollFrequency payFrequency;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate effectiveFrom;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant createdAt;
}