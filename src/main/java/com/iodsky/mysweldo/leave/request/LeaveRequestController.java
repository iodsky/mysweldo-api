package com.iodsky.mysweldo.leave.request;

import com.iodsky.mysweldo.common.RequestStatus;
import com.iodsky.mysweldo.common.response.*;
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

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Leave request management endpoints")
public class LeaveRequestController {

    private final LeaveRequestService service;
    private final LeaveRequestMapper mapper;

    @PostMapping
    @Operation(summary = "Create leave request", description = "Submit a new leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> createLeaveRequest(@Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.createLeaveRequest(dto));
        return ResponseFactory.created("Leave request created successfully", leaveRequest);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get leave requests", description = "Retrieve a paginated list of leave requests for the authenticated employee")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Page<LeaveRequest> page = service.getLeaveRequests(startDate, endDate, pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return ResponseFactory.ok("Leave requests retrieved successfully", leaveRequests, PaginationMeta.of(page));
    }

    @GetMapping("/subordinates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getSubordinatesLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<LeaveRequest> page = service.getSubordinatesLeaveRequests(pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return ResponseFactory.ok("Leave requests retrieved successfully", leaveRequests, PaginationMeta.of(page));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getEmployeeLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<LeaveRequest> page = service.getEmployeeLeaveRequests(pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return ResponseFactory.ok("Leave requests retrieved successfully", leaveRequests, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by ID", description = "Retrieve a specific leave request by its ID")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> getLeaveRequestById(
            @Parameter(description = "Leave request ID") @PathVariable String id) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.getLeaveRequestById(id));
        return ResponseFactory.ok("Leave request retrieved successfully", leaveRequest);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update leave request", description = "Update an existing leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> updateLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String id,
            @Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.updateLeaveRequest(id, dto));
        return ResponseFactory.ok("Leave request updated successfully", leaveRequest);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'SUPERUSER')")
    @Operation(summary = "Update leave status", description = "Approve or reject a leave request. Requires HR role.")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> updateLeaveStatus(
            @Parameter(description = "Leave request ID") @PathVariable String id,
            @RequestParam RequestStatus status) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.updateLeaveStatus(id, status));
        return ResponseFactory.ok("Leave status updated successfully", leaveRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete leave request", description = "Cancel a leave request")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String id) {
        service.deleteLeaveRequest(id);
        DeleteResponse res = new DeleteResponse("Leave Request", id);
        return ResponseFactory.ok("Leave request deleted successfully", res);
    }

}
