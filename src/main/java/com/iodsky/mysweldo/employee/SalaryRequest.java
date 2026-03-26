package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SalaryRequest {

    @NotNull(message = "Base rate is required")
    @DecimalMin(value = "0.00", message = "baseRate cannot be nagative")
    private BigDecimal rate;

    @NotNull(message = "Pay type is required")
    private PayType payType;

    @NotNull(message = "Payroll frequency is required")
    private PayrollFrequency payrollFrequency;

}
