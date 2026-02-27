package com.iodsky.sweldox.payroll.core;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreatePayrollRequest {
    @NotNull
    private Long employeeId;
    @Past
    private LocalDate periodStartDate;
    @PastOrPresent
    private LocalDate periodEndDate;
    @Future
    private LocalDate payDate;
}
