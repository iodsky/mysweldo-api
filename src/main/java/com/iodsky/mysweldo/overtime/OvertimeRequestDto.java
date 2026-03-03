package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.RequestStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class OvertimeRequestDto {
    private UUID id;
    private Long employeeId;
    private LocalDate date;
    private BigDecimal overtimeHours;
    private String reason;
    private RequestStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID lastModifiedBy;
}
