package com.iodsky.mysweldo.batch.employee;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployeeBenefitRequest {

    @NotNull(message = "Benefit code id is requried")
    private String benefitCode;

    @NotNull(message = "Amount is required")
    @Min(100)
    private BigDecimal amount;

}
