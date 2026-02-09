package com.iodsky.sweldox.leave.request;

import com.iodsky.sweldox.common.response.*;
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

@RestController
@RequestMapping("/leave-requests")
@RequiredArgsConstructor
@Tag(name = "Leave Requests", description = "Leave request management endpoints")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;
    private final LeaveRequestMapper leaveRequestMapper;

    @PostMapping
    @Operation(summary = "Create leave request", description = "Submit a new leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> createLeaveRequest(@Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = leaveRequestMapper.toDto(leaveRequestService.createLeaveRequest(dto));
        return ResponseFactory.created("Leave request created successfully", leaveRequest);
    }

    @GetMapping
    @Operation(summary = "Get leave requests", description = "Retrieve a paginated list of leave requests for the authenticated employee")
    public ResponseEntity<ApiResponse<List<LeaveRequestDto>>> getLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<LeaveRequest> page = leaveRequestService.getLeaveRequests(pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(leaveRequestMapper::toDto).toList();
        return ResponseFactory.ok("Leave requests retrieved successfully", leaveRequests, PaginationMeta.of(page));
    }

    @GetMapping("/{leaveRequestId}")
    @Operation(summary = "Get leave request by ID", description = "Retrieve a specific leave request by its ID")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> getLeaveRequestById(
            @Parameter(description = "Leave request ID") @PathVariable String leaveRequestId) {
        LeaveRequestDto leaveRequest = leaveRequestMapper.toDto(leaveRequestService.getLeaveRequestById(leaveRequestId));
        return ResponseFactory.ok("Leave request retrieved successfully", leaveRequest);
    }

    @PutMapping("/{leaveRequestId}")
    @Operation(summary = "Update leave request", description = "Update an existing leave request")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> updateLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String leaveRequestId,
            @Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = leaveRequestMapper.toDto(leaveRequestService.updateLeaveRequest(leaveRequestId, dto));
        return ResponseFactory.ok("Leave request updated successfully", leaveRequest);
    }

    @PreAuthorize("hasRole('HR')")
    @PatchMapping("/{leaveRequestId}/status")
    @Operation(summary = "Update leave status", description = "Approve or reject a leave request. Requires HR role.")
    public ResponseEntity<ApiResponse<LeaveRequestDto>> updateLeaveStatus(
            @Parameter(description = "Leave request ID") @PathVariable String leaveRequestId,
            @Valid @RequestBody UpdateLeaveStatusDto dto) {
        LeaveRequestDto leaveRequest = leaveRequestMapper.toDto(leaveRequestService.updateLeaveStatus(leaveRequestId, dto.getStatus()));
        return ResponseFactory.ok("Leave status updated successfully", leaveRequest);
    }

    @DeleteMapping("/{leaveRequestId}")
    @Operation(summary = "Delete leave request", description = "Cancel a leave request")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String leaveRequestId) {
        leaveRequestService.deleteLeaveRequest(leaveRequestId);
        DeleteResponse res = new DeleteResponse("Leave Request", leaveRequestId);
        return ResponseFactory.ok("Leave request deleted successfully", res);
    }
}
