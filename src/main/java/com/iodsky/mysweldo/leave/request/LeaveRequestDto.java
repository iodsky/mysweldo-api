package com.iodsky.mysweldo.leave.request;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class LeaveRequestDto {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @NotNull(message = "Leave type is required")@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String leaveType;

    private Long employeeId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant requestDate;

    @NotNull(message = "Start date is required")@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull(message = "End date is required")@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @FutureOrPresent
    private LocalDate endDate;

    @Size(max = 250)
    private String note;

    private String status;
}
