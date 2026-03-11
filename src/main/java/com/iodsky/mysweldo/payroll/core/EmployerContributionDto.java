package com.iodsky.mysweldo.payroll.core;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EmployerContributionDto {

    private String contribution;
    private BigDecimal amount;

}
