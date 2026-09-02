package com.iodsky.mysweldo.security.user;

import com.iodsky.mysweldo.common.response.PageDto;
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
import org.springframework.web.bind.annotation.*;

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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create user",
            description = "Create a new user account. Requires IT role.",
            operationId = "createUser"
    )
    public UserDto createUser(@Valid @RequestBody UserRequest userRequest) {
        return mapper.toDto(service.createUser(userRequest));
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a paginated list of user accounts with optional role filtering. Requires IT role.", operationId = "getUsers")
    public PageDto<UserDto> getUsers(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by role") @RequestParam(required = false) String roleName
    ) {
        Page<User> page = service.getAllUsers(pageNo, limit, roleName);
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieve a specific user by their ID. Requires IT role.", operationId = "getUserById")
    public UserDto getUserById(@PathVariable UUID id) {
        return mapper.toDto(service.getUserById(id));
    }

    @PatchMapping("/{id}/role")
    @Operation(summary = "Update user role", description = "Update the role of a specific user. Requires IT role.", operationId = "updateUserRole")
    public UserDto updateUserRole(@PathVariable UUID id, @RequestParam String role) {
        return mapper.toDto(service.updateUserRole(id, role));
    }

}
