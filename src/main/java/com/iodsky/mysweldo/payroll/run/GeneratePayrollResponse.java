package com.iodsky.mysweldo.payroll.run;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneratePayrollResponse {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PayrollRunDto payrollRun;

    private List<Long> skippedEmployeeIds;

}
