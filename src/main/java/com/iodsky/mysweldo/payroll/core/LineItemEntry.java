package com.iodsky.mysweldo.payroll.core;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LineItemEntry {

    @NotNull
    private String code;
    private BigDecimal amount;

}
