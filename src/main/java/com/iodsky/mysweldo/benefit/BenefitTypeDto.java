package com.iodsky.mysweldo.benefit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitTypeDto {
    private String id;
    private String type;
    private Instant createdAt;
    private Instant updatedAt;
}
