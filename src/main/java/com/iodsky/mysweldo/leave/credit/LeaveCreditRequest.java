package com.iodsky.mysweldo.leave.credit;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class LeaveCreditRequest {
    @NotNull(message = "EmployeeId is required")
    private Long employeeId;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
