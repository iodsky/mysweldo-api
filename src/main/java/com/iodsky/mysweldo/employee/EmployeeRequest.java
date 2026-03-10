package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.batch.employee.EmployeeBenefitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class EmployeeRequest {

    @NotEmpty(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotEmpty(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotNull(message = "Birthday is required")
    @Past(message = "Invalid birthday")
    private LocalDate birthday;

    @NotEmpty(message = "Address is required")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @NotEmpty(message = "Phone number is required")
    private String phoneNumber;

    @NotNull(message = "Government IDs are required")
    @Valid
    private GovernmentIdRequest governmentId;

    private Long supervisorId;

    @NotNull(message = "Position is required")
    private String positionId;

    @NotNull(message = "Department is required")
    private String departmentId;

    @NotNull(message = "Status is required")
    private Status status;

    @NotNull(message = "Start shift is required")
    private LocalTime startShift;

    @NotNull(message = "End shift is required")
    private LocalTime endShift;

    @NotNull
    @Positive
    private BigDecimal basicSalary;

    @NotNull
    private List<EmployeeBenefitRequest> benefits;

}
