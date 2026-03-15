package com.iodsky.mysweldo.deduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeductionDto {
    private String code;
    private String description;
    private Boolean statutory;
    private Instant createdAt;
    private Instant updatedAt;
}
