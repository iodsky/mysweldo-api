package com.iodsky.sweldox.leave.credit;

import com.iodsky.sweldox.common.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave-credits")
@RequiredArgsConstructor
@Tag(name = "Leave Credits", description = "Leave credit management endpoints")
public class LeaveCreditController {

    private final LeaveCreditService leaveCreditService;
    private final LeaveCreditMapper leaveCreditMapper;

    @PreAuthorize("hasRole('HR')")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Creates employee leave credits",
            description = "Set up initial leave credits for an employee. Requires HR role. Returns a list of created leave credits.",
            operationId = "initializeEmployeeLeaveCredits"
    )
    public ResponseEntity<ApiResponse<List<LeaveCreditDto>>> createLeaveCredits(@Valid @RequestBody LeaveCreditRequest dto) {
        List<LeaveCreditDto> leaveCredits = leaveCreditService.createLeaveCredits(dto)
                .stream()
                .map(leaveCreditMapper::toDto)
                .toList();
        return ResponseFactory.created("Leave credits created successfully", leaveCredits);
    }

    @GetMapping
    @Operation(summary = "Get my leave credits", description = "Retrieve leave credits for the authenticated employee")
    public ResponseEntity<ApiResponse<List<LeaveCreditDto>>> getLeaveCredits() {
        List<LeaveCreditDto> credits = leaveCreditService.getLeaveCreditsByEmployeeId()
                .stream().map(leaveCreditMapper::toDto).toList();
        return ResponseFactory.ok("Leave credits retrieved successfully", credits);
    }

    @PreAuthorize("hasRole('HR')")
    @DeleteMapping("/employee/{employeeId}")
    @Operation(summary = "Delete employee leave credits", description = "Delete all leave credits for a specific employee. Requires HR role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteLeaveCreditsByEmployeeId(@Parameter(description = "Employee ID") @PathVariable Long employeeId) {
        leaveCreditService.deleteLeaveCreditsByEmployeeId(employeeId);
        DeleteResponse res = new DeleteResponse("Leave Credits", employeeId);
        return ResponseFactory.ok("Employee leave credits deleted successfully", res);
    }

}
