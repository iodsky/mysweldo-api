package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.common.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
@Validated
@RequiredArgsConstructor
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService service;
    private final EmployeeMapper mapper;

    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new employee",
            description = "Create a new employee record. Requires HR role.",
            operationId = "createEmployee"
    )
    public ApiResponse<EmployeeDto> createEmployee(@Valid @RequestBody EmployeeRequest request) {
        EmployeeDto employee = mapper.toDto(service.createEmployee(request));
        return ResponseFactory.success("Employee created successfully", employee);
    }

    @PreAuthorize("hasAnyRole('HR', 'IT', 'PAYROLL', 'SUPERUSER')")
    @GetMapping
    @Operation(summary = "Get all employees", description = "Retrieve a paginated list of employees with optional filters. Requires HR, IT, or PAYROLL role.")
    public ApiResponse<List<EmployeeDto>> getAllEmployees(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by department") @RequestParam(required = false) String department,
            @Parameter(description = "Filter by supervisor ID") @RequestParam(required = false) @Positive Long supervisor,
            @Parameter(description = "Filter by employment status") @RequestParam(required = false) String status
    ) {
        Page<Employee> page = service.getAllEmployees(pageNo, limit, department, supervisor, status);

        List<EmployeeDto> employees = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.success(
                "Employees retrieved successfully",
                employees,
                PaginationMeta.of(page)
        );

    }

    @GetMapping("/me")
    @Operation(summary = "Get current employee", description = "Retrieve the authenticated employee's information")
    public ApiResponse<EmployeeDto> getAuthenticatedEmployee() {
        EmployeeDto employee =  mapper.toDto(service.getAuthenticatedEmployee());
        return ResponseFactory.success("Employee retrieved successfully", employee);
    }

    @PreAuthorize("hasAnyRole('HR', 'IT', 'PAYROLL', 'SUPERUSER')")
    @GetMapping("/{id}")
    @Operation(summary = "Get employee by ID", description = "Retrieve a specific employee by their ID. Requires HR, IT, or PAYROLL role.")
    public ApiResponse<EmployeeDto> getEmployeeById(@Parameter(description = "Employee ID") @PathVariable long id) {
        EmployeeDto employee = mapper.toDto(service.getEmployeeById(id));
        return ResponseFactory.success("Employee retrieved successfully", employee);
    }

    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Update an existing employee's information. Requires HR role.")
    public ApiResponse<EmployeeDto> updateEmployee(@Parameter(description = "Employee ID") @PathVariable long id, @Valid @RequestBody EmployeeRequest request) {
        EmployeeDto employee = mapper.toDto(service.updateEmployeeById(id, request));
        return ResponseFactory.success("Employee updated successfully", employee);
    }

    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee", description = "Delete or deactivate an employee. Requires HR role.")
    public ApiResponse<Void> deleteEmployee(@Parameter(description = "Employee ID") @PathVariable long id, @Parameter(description = "Status to set (INACTIVE or TERMINATED)") @RequestParam EmploymentStatus status) {
        service.deleteEmployeeById(id, status);
        return ResponseFactory.success("Employee deleted successfully");
    }
}
