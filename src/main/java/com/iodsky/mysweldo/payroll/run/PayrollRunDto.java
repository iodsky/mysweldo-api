package com.iodsky.mysweldo.payroll.run;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PayrollRunDto {

    private UUID id;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private PayrollFrequency payrollFrequency;
    private PayrollRunType type;
    private PayrollRunStatus status;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalBenefits;
    private BigDecimal totalEmployerCost;
    private String notes;

}
