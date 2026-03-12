package com.iodsky.mysweldo.payroll.core;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePayrollContributionRequest {

    @NotNull
    @NotEmpty
    List<LineItemEntry> contributions;

}
