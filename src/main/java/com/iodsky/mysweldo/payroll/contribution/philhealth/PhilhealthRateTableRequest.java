package com.iodsky.mysweldo.payroll.contribution.philhealth;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhilhealthRateTableRequest {

    @NotNull(message = "Premium rate is required")
    @DecimalMin(value = "0.0001", message = "Premium rate must be greater than 0")
    @DecimalMax(value = "1.0", message = "Premium rate must not exceed 1 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Premium rate must have at most 4 decimal places")
    private BigDecimal premiumRate;

    @NotNull(message = "Max salary cap is required")
    @DecimalMin(value = "0.01", message = "Max salary cap must be greater than 0")
    private BigDecimal maxSalaryCap;

    @NotNull(message = "Min salary floor is required")
    @DecimalMin(value = "0.01", message = "Min salary floor must be greater than 0")
    private BigDecimal minSalaryFloor;

    @NotNull(message = "Fixed contribution is required")
    @DecimalMin(value = "0.01", message = "Fixed contribution must be greater than 0")
    private BigDecimal fixedContribution;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
