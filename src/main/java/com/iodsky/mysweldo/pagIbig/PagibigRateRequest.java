package com.iodsky.mysweldo.pagIbig;

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
public class PagibigRateRequest {

    @NotNull(message = "Employee rate is required")
    @DecimalMin(value = "0.0001", message = "Employee rate must be greater than 0")
    @DecimalMax(value = "1.0", message = "Employee rate must not exceed 1 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Employee rate must have at most 4 decimal places")
    private BigDecimal employeeRate;

    @NotNull(message = "Employer rate is required")
    @DecimalMin(value = "0.0001", message = "Employer rate must be greater than 0")
    @DecimalMax(value = "1.0", message = "Employer rate must not exceed 1 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Employer rate must have at most 4 decimal places")
    private BigDecimal employerRate;

    @DecimalMin(value = "0.01", message = "Low income threshold must be greater than 0")
    private BigDecimal lowIncomeThreshold;

    @DecimalMin(value = "0.0001", message = "Low income employee rate must be greater than 0")
    @DecimalMax(value = "1.0", message = "Low income employee rate must not exceed 1 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Low income employee rate must have at most 4 decimal places")
    private BigDecimal lowIncomeEmployeeRate;

    @NotNull(message = "Max salary cap is required")
    @DecimalMin(value = "0.01", message = "Max salary cap must be greater than 0")
    private BigDecimal maxSalaryCap;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
