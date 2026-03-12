package com.iodsky.mysweldo.department;

import com.iodsky.mysweldo.common.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/departments")
@PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department management endpoints")
public class DepartmentController {

    private final DepartmentService service;
    private final DepartmentMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a new department",
            description = "Create a new department. Requires HR role.",
            operationId = "createDepartment"
    )
    public ApiResponse<DepartmentDto> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        DepartmentDto department = mapper.toDto(service.createDepartment(request));
        return ResponseFactory.success("Department created successfully", department);
    }

    @GetMapping
    @Operation(summary = "Get all departments", description = "Retrieve a paginated list of departments. Requires HR role.")
    public ApiResponse<List<DepartmentDto>> getAllDepartments(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<Department> page = service.getAllDepartments(pageNo, limit);

        List<DepartmentDto> departments = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "Departments retrieved successfully",
                departments,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID", description = "Retrieve a specific department by its ID. Requires HR role.")
    public ApiResponse<DepartmentDto> getDepartmentById(
            @Parameter(description = "Department ID") @PathVariable String id
    ) {
        DepartmentDto department = mapper.toDto(service.getDepartmentById(id));
        return ResponseFactory.success("Department retrieved successfully", department);
    }

    @PreAuthorize("hasRole('HR')")
    @PutMapping("/{id}")
    @Operation(summary = "Update department", description = "Update an existing department's information. Requires HR role.")
    public ApiResponse<DepartmentDto> updateDepartment(
            @Parameter(description = "Department ID") @PathVariable String id,
            @Valid @RequestBody DepartmentUpdateRequest request
    ) {
        DepartmentDto department = mapper.toDto(service.updateDepartment(id, request));
        return ResponseFactory.success("Department updated successfully", department);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department", description = "Delete a department. Requires HR role.")
    public ApiResponse<Void> deleteDepartment(
            @Parameter(description = "Department ID") @PathVariable String id
    ) {
        service.deleteDepartment(id);
        return ResponseFactory.success("Department deleted successfully");
    }
}
