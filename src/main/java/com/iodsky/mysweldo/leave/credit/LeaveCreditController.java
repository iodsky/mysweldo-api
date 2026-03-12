package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.common.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave-credits")
@RequiredArgsConstructor
@Tag(name = "Leave Credits", description = "Leave credit management endpoints")
public class LeaveCreditController {

    private final LeaveCreditService service;
    private final LeaveCreditMapper mapper;

    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Creates employee leave credits",
            description = "Set up initial leave credits for an employee. Requires HR role. Returns a list of created leave credits.",
            operationId = "initializeEmployeeLeaveCredits"
    )
    public ApiResponse<List<LeaveCreditDto>> createLeaveCredits(@Valid @RequestBody LeaveCreditRequest dto) {
        List<LeaveCreditDto> leaveCredits = service.createLeaveCredits(dto)
                .stream().map(mapper::toDto).toList();
        return ResponseFactory.success("Leave credits created successfully", leaveCredits);
    }

    @GetMapping
    @Operation(summary = "Get my leave credits", description = "Retrieve leave credits for the authenticated employee")
    public ApiResponse<List<LeaveCreditDto>> getLeaveCredits() {
        List<LeaveCreditDto> credits = service.getLeaveCreditsByEmployeeId()
                .stream().map(mapper::toDto).toList();
        return ResponseFactory.success("Leave credits retrieved successfully", credits);
    }

    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @DeleteMapping("/employee/{employeeId}")
    @Operation(summary = "Delete employee leave credits", description = "Delete all leave credits for a specific employee. Requires HR role.")
    public ApiResponse<Void> deleteLeaveCreditsByEmployeeId(
            @Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        service.deleteLeaveCreditsByEmployeeId(employeeId);
        return ResponseFactory.success("Employee leave credits deleted successfully");
    }
}
