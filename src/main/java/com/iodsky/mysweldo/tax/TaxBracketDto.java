package com.iodsky.mysweldo.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaxBracketDto {
    private UUID id;
    private BigDecimal minIncome;
    private BigDecimal maxIncome;
    private BigDecimal baseTax;
    private BigDecimal marginalRate;
    private BigDecimal threshold;
    private LocalDate effectiveDate;
    private Instant createdAt;
    private Instant updatedAt;
}
