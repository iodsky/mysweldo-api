package com.iodsky.mysweldo.overtime;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.iodsky.mysweldo.common.RequestStatus;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class OvertimeRequestDto {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    private Long employeeId;

    @NotNull(message = "Date is required")@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @Past
    private LocalDate date;

    @NotNull
    @DecimalMin(value = "0.01", message = "Overtime hours must be greater than 0")
    @DecimalMax(value = "24.00", message = "Overtime hours must be less than 24 hours")@Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal overtimeHours;
 
    private String reason;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private RequestStatus status;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant createdAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Instant updatedAt;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID createdBy;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private UUID lastModifiedBy;
}
