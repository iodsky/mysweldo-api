package com.iodsky.mysweldo.payroll.core;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollBenefitDto {
    private String benefit;
    private BigDecimal amount;
}
