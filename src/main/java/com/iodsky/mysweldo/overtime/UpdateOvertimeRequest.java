package com.iodsky.mysweldo.overtime;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class UpdateOvertimeRequest {

    @NotNull(message = "Date is required")
    @Past
    private LocalDate date;

    private String reason;

}
