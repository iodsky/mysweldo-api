package com.iodsky.mysweldo.employee;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class EmployeeDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate birthday;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String address;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String phoneNumber;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String sssNumber;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String tinNumber;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String philhealthNumber;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String pagIbigNumber;
    private EmployeeBasicDto supervisor;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private PositionBasicDto position;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private DepartmentBasicDto department;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String status;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String type;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime startShift;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime endShift;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private SalaryDto salary;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private List<EmployeeBenefitDto> benefits;

}
