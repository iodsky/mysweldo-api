package com.iodsky.mysweldo.leave.credit;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmployeeLeaveCreditDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long employeeId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<CreditSummary> credits;
}
