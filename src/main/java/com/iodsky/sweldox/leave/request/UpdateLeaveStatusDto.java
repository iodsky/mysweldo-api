package com.iodsky.sweldox.leave.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLeaveStatusDto {
    @NotNull(message = "Status is required")
    private LeaveStatus status;
}
