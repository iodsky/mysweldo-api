package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.RequestStatus;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("overtime-requests")
@Validated
@RequiredArgsConstructor
@Tag(name = "Overtime Requests", description = "Manage employee overtime requests")
public class OvertimeRequestController {

    private final OvertimeRequestService service;
    private final OvertimeRequestMapper mapper;

    @PostMapping
    @Operation(summary = "Create overtime request", description = "Create a new overtime request for the authenticated employee")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> createOvertimeRequest(@Valid @RequestBody  AddOvertimeRequest request) {
        OvertimeRequest entity = service.createOvertimeRequest(request);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.created("Overtime request successfully created", dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get all overtime requests", description = "Retrieve all overtime requests with optional date filters and pagination. Requires HR or SUPERUSER role.")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getOvertimeRequests(
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getOvertimeRequests(startDate, endDate, pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get overtime request by ID", description = "Retrieve a specific overtime request by its ID")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> getOvertimeRequestById(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id) {
        OvertimeRequest entity = service.getOvertimeRequestById(id);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request retrieved successfully", dto);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my overtime requests", description = "Retrieve overtime requests for the authenticated employee with pagination")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getEmployeeOvertimeRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getEmployeeOvertimeRequest(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @GetMapping("/subordinates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Get subordinates' overtime requests", description = "Retrieve overtime requests for employees supervised by the authenticated user. Requires SUPERVISOR role.")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getSubordinatesOvertimeRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getSubordinatesOvertimeRequests(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update overtime request", description = "Update an existing overtime request")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> updateOvertimeRequest(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id,
            @Valid @RequestBody UpdateOvertimeRequest request) {
        OvertimeRequest entity = service.updateOvertimeRequest(id, request);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request updated successfully", dto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'SUPERUSER')")
    @Operation(summary = "Update overtime request status", description = "Update the status of an overtime request (PENDING, APPROVED, REJECTED). Requires HR, SUPERVISOR, or SUPERUSER role.")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> updateOvertimeRequestStatus(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id,
            @Parameter(description = "New status (PENDING, APPROVED, REJECTED)") @RequestParam RequestStatus status) {
        OvertimeRequest entity = service.updateOvertimeRequestStatus(id, status);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request status updated successfully", dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete overtime request", description = "Soft delete an overtime request")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteOvertimeRequest(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id) {
        service.deleteOvertimeRequest(id);

        DeleteResponse res = DeleteResponse.builder()
                .resourceType("Overtime Request")
                .resourceId(id)
                .build();

        return ResponseFactory.ok("Overtime request deleted successfully", res);
    }

}
