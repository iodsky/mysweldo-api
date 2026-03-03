package com.iodsky.mysweldo.benefit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BenefitRequest {

    @NotNull(message = "Benefit type id is requried")
    private String benefitTypeId;

    @NotNull(message = "Amount is required")
    @Min(100)
    private BigDecimal amount;
}
