package com.iodsky.mysweldo.leave.credit;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmployeeLeaveCreditDto {
    private Long employeeId;
    private String firstName;
    private String lastName;
    private List<CreditSummary> credits;
}
