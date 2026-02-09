package com.iodsky.sweldox.leave.credit;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class LeaveCreditDto {

    private UUID id;
    private Long employeeId;
    private String type;
    private double credits;
    private LocalDate effectiveDate;
}
