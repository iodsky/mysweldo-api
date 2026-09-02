package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.RequestStatus;

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
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create overtime request", description = "Create a new overtime request for the authenticated employee", operationId = "createOvertimeRequest")
    public OvertimeRequestDto createOvertimeRequest(@Valid @RequestBody OvertimeRequestDto request) {
        OvertimeRequest entity = service.createOvertimeRequest(request);
        return mapper.toDto(entity);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get all overtime requests", description = "Retrieve all overtime requests with optional date filters and pagination. Requires HR or SUPERUSER role.", operationId = "getOvertimeRequests")
    public PageDto<OvertimeRequestDto> getOvertimeRequests(
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getOvertimeRequests(startDate, endDate, pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get overtime request by ID", description = "Retrieve a specific overtime request by its ID", operationId = "getOvertimeRequestById")
    public OvertimeRequestDto getOvertimeRequestById(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id) {
        OvertimeRequest entity = service.getOvertimeRequestById(id);
        return mapper.toDto(entity);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my overtime requests", description = "Retrieve overtime requests for the authenticated employee with pagination", operationId = "getMyOvertimeRequests")
    public PageDto<OvertimeRequestDto> getEmployeeOvertimeRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getEmployeeOvertimeRequest(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/subordinates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    @Operation(summary = "Get subordinates' overtime requests", description = "Retrieve overtime requests for employees supervised by the authenticated user. Requires SUPERVISOR role.", operationId = "getSubordinatesOvertimeRequests")
    public PageDto<OvertimeRequestDto> getSubordinatesOvertimeRequests(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getSubordinatesOvertimeRequests(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update overtime request", description = "Update an existing overtime request", operationId = "updateOvertimeRequest")
    public OvertimeRequestDto updateOvertimeRequest(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id,
            @Valid @RequestBody OvertimeRequestDto request) {
        OvertimeRequest entity = service.updateOvertimeRequest(id, request);
        return mapper.toDto(entity);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR', 'SUPERUSER')")
    @Operation(summary = "Update overtime request status", description = "Update the status of an overtime request (PENDING, APPROVED, REJECTED). Requires HR, SUPERVISOR, or SUPERUSER role.", operationId = "updateOvertimeRequestStatus")
    public OvertimeRequestDto updateOvertimeRequestStatus(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id,
            @Parameter(description = "New status (PENDING, APPROVED, REJECTED)") @RequestParam RequestStatus status) {
        OvertimeRequest entity = service.updateOvertimeRequestStatus(id, status);
        return mapper.toDto(entity);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete overtime request", description = "Soft delete an overtime request", operationId = "deleteOvertimeRequest")
    public void deleteOvertimeRequest(
            @Parameter(description = "Overtime request ID") @PathVariable UUID id) {
        service.deleteOvertimeRequest(id);

        
    }
}
