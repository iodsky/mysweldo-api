package com.iodsky.mysweldo.attendance;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long employeeId;
    private String employeeFirstName;
    private String employeeLastName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime timeIn;
    private LocalDateTime timeOut;
    private BigDecimal totalHours;
}