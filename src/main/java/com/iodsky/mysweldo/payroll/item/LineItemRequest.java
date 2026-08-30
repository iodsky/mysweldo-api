package com.iodsky.mysweldo.payroll.item;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class LineItemRequest {

    @NotNull
    private String code;
    private BigDecimal amount;

}
