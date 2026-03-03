package com.iodsky.mysweldo.payroll.tax;

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
public class IncomeTaxBracketRequest {

    @NotNull(message = "Minimum income is required")
    @DecimalMin(value = "0.00", message = "Minimum income must be greater than or equal to 0")
    private BigDecimal minIncome;

    private BigDecimal maxIncome;

    @NotNull(message = "Base tax is required")
    @DecimalMin(value = "0.00", message = "Base tax must be greater than or equal to 0")
    private BigDecimal baseTax;

    @NotNull(message = "Marginal rate is required")
    @DecimalMin(value = "0.0000", message = "Marginal rate must be greater than or equal to 0")
    @DecimalMax(value = "1.0", message = "Marginal rate must not exceed 1 (100%)")
    @Digits(integer = 1, fraction = 4, message = "Marginal rate must have at most 4 decimal places")
    private BigDecimal marginalRate;

    @NotNull(message = "Threshold is required")
    @DecimalMin(value = "0.00", message = "Threshold must be greater than or equal to 0")
    private BigDecimal threshold;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;
}
