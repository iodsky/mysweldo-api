package com.iodsky.sweldox.overtime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AddOvertimeRequest {

    private Long employeeId;

    @NotNull(message = "Date is required")
    @Past
    private LocalDate date;

    private String reason;

}
