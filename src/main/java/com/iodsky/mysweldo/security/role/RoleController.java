package com.iodsky.mysweldo.security.role;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.DeleteResponse;
import com.iodsky.mysweldo.common.response.PaginationMeta;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/roles")
@PreAuthorize("hasAnyRole('IT', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Manage user roles and permissions. Requires IT or SUPERUSER role.")
public class RoleController {

    private final RoleService service;
    private final RoleMapper mapper;

    @PostMapping
    @Operation(summary = "Create role", description = "Create a new user role. Requires IT or SUPERUSER role.")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(@Valid @RequestBody RoleRequest request) {
        Role role = service.createRole(request);
        RoleDto dto = mapper.toDto(role);

        return ResponseFactory.ok("Role created succesfully", dto);
    }

    @GetMapping
    @Operation(summary = "Get all roles", description = "Retrieve all user roles with pagination. Requires IT or SUPERUSER role.")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        Page<Role> page = service.getAllRoles(pageNo, limit);
        List<RoleDto> data = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Roles retrieved successfully", data, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Retrieve a specific role by its ID. Requires IT or SUPERUSER role.")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        Role role = service.getRoleById(id);
        RoleDto dto = mapper.toDto(role);

        return ResponseFactory.ok("Role retrieved successfully", dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role. Requires IT or SUPERUSER role.")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @Parameter(description = "Role ID") @PathVariable Long id,
            @Valid @RequestBody RoleRequest request) {
        Role role = service.updateRole(id, request);
        RoleDto dto = mapper.toDto(role);

        return ResponseFactory.ok("Role updated successfully", dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Soft delete a role. Requires IT or SUPERUSER role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteRole(
            @Parameter(description = "Role ID") @PathVariable Long id) {
        service.deleteRole(id);

        DeleteResponse res = DeleteResponse.builder()
                .resourceType("Role")
                .resourceId(id)
                .build();

        return ResponseFactory.ok("Role deleted successfully", res);
    }

}
