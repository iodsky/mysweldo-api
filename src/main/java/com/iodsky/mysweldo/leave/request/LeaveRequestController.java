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
import org.springframework.http.HttpStatus;
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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create leave request", description = "Submit a new leave request", operationId = "createLeaveRequest")
    public LeaveRequestDto createLeaveRequest(@Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.createLeaveRequest(dto));
        return leaveRequest;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get leave requests", description = "Retrieve a paginated list of leave requests", operationId = "getLeaveRequests")
    public PageDto<LeaveRequestDto> getLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate
    ) {
        Page<LeaveRequest> page = service.getLeaveRequests(startDate, endDate, pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/subordinates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Get subordinates' leave requests", description = "Retrieve leave requests for employees supervised by the authenticated user. Requires SUPERVISOR role.", operationId = "getSubordinatesLeaveRequests")
    public PageDto<LeaveRequestDto> getSubordinatesLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<LeaveRequest> page = service.getSubordinatesLeaveRequests(pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my leave requests", description = "Retrieve leave requests for the authenticated employee", operationId = "getMyLeaveRequests")
    public PageDto<LeaveRequestDto> getEmployeeLeaveRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<LeaveRequest> page = service.getEmployeeLeaveRequests(pageNo, limit);
        List<LeaveRequestDto> leaveRequests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get leave request by ID", description = "Retrieve a specific leave request by its ID", operationId = "getLeaveRequestById")
    public LeaveRequestDto getLeaveRequestById(
            @Parameter(description = "Leave request ID") @PathVariable String id) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.getLeaveRequestById(id));
        return leaveRequest;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update leave request", description = "Update an existing leave request", operationId = "updateLeaveRequest")
    public LeaveRequestDto updateLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String id,
            @Valid @RequestBody LeaveRequestDto dto) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.updateLeaveRequest(id, dto));
        return leaveRequest;
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'SUPERUSER')")
    @Operation(summary = "Update leave status", description = "Approve or reject a leave request. Requires HR role.", operationId = "updateLeaveRequestStatus")
    public LeaveRequestDto updateLeaveStatus(
            @Parameter(description = "Leave request ID") @PathVariable String id,
            @RequestParam RequestStatus status) {
        LeaveRequestDto leaveRequest = mapper.toDto(service.updateLeaveStatus(id, status));
        return leaveRequest;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete leave request", description = "Cancel a leave request", operationId = "deleteLeaveRequest")
    public void deleteLeaveRequest(
            @Parameter(description = "Leave request ID") @PathVariable String id) {
        service.deleteLeaveRequest(id);
        
    }
}
