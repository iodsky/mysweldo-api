package com.iodsky.mysweldo.security.user;

import com.iodsky.mysweldo.common.response.ApiResponse;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasAnyRole('SUPERUSER','IT')")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management endpoints")
public class UserController {

    private final UserService service;
    private final UserMapper mapper;

    @PostMapping
    @Operation(
            summary = "Create user",
            description = "Create a new user account. Requires IT role.",
            operationId = "createUser"
    )
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserRequest userRequest) {
        UserDto user = mapper.toDto(service.createUser(userRequest));
        return ResponseFactory.created("User created successfully", user);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a paginated list of user accounts with optional role filtering. Requires IT role.")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by role") @RequestParam(required = false) String roleName
    ) {
        Page<User> page  = service.getAllUsers(pageNo, limit, roleName);
        List<UserDto> data = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Users retrieved successfully", data, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        User user = service.getUserById(id);
        UserDto res = mapper.toDto(user);
        return ResponseFactory.ok("User retrieved successfully successfully", res);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(@PathVariable UUID id, @RequestParam String role) {
        User user = service.updateUserRole(id, role);
        UserDto res = mapper.toDto(user);
        return ResponseFactory.ok("User's role updated successfully", res);
    }

}
