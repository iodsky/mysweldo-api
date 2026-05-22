package com.iodsky.mysweldo.leave.credit;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreditSummary {
    private String type;
    private double credits;
}
