package com.iodsky.mysweldo.payroll.contribution.sss;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SssRateTableRequest {

    @NotNull(message = "Total SSS contribution is required")
    @DecimalMin(value = "0.01", message = "Total SSS must be greater than 0")
    private BigDecimal totalSss;

    @NotNull(message = "Employee SSS rate is required")
    @DecimalMin(value = "0.0001", message = "Employee SSS rate must be greater than 0")
    @DecimalMax(value = "1.0000", message = "Employee SSS rate must be less than or equal to 1")
    private BigDecimal employeeRate;

    @NotNull(message = "Employer SSS rate is required")
    @DecimalMin(value = "0.0001", message = "Employer SSS rate must be greater than 0")
    @DecimalMax(value = "1.0000", message = "Employer SSS rate must be less than or equal to 1")
    private BigDecimal employerRate;

    @NotNull(message = "Salary brackets are required")
    @Size(min = 1, message = "At least one salary bracket is required")
    private List<SalaryBracketRequest> salaryBrackets;

    @NotNull(message = "Effective date is required")
    private LocalDate effectiveDate;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SalaryBracketRequest {
        @NotNull(message = "Minimum salary is required")
        @DecimalMin(value = "0.00", message = "Minimum salary must be greater than or equal to 0")
        private BigDecimal minSalary;

        private BigDecimal maxSalary;

        @NotNull(message = "Monthly Salary Credit (MSC) is required")
        @DecimalMin(value = "0.01", message = "MSC must be greater than 0")
        private BigDecimal msc;
    }
}
