package com.iodsky.sweldox.overtime;

import com.iodsky.sweldox.common.RequestStatus;
import com.iodsky.sweldox.common.response.ApiResponse;
import com.iodsky.sweldox.common.response.DeleteResponse;
import com.iodsky.sweldox.common.response.PaginationMeta;
import com.iodsky.sweldox.common.response.ResponseFactory;
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
public class OvertimeRequestController {

    private final OvertimeRequestService service;
    private final OvertimeRequestMapper mapper;

    @PostMapping
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> createOvertimeRequest(@Valid @RequestBody  AddOvertimeRequest request) {
        OvertimeRequest entity = service.createOvertimeRequest(request);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.created("Overtime request successfully created", dto);
    }

    @GetMapping
    @PreAuthorize("hasRole('HR')")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getOvertimeRequests(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getOvertimeRequests(startDate, endDate, pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> getOvertimeRequestById(@PathVariable UUID id) {
        OvertimeRequest entity = service.getOvertimeRequestById(id);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request retrieved successfully", dto);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getEmployeeOvertimeRequests(
            @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getEmployeeOvertimeRequest(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @GetMapping("/subordinates")
    @PreAuthorize("hasRole('SUPERVISOR')")
    public ResponseEntity<ApiResponse<List<OvertimeRequestDto>>> getSubordinatesOvertimeRequests(
            @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<OvertimeRequest> page = service.getSubordinatesOvertimeRequests(pageNo, limit);
        List<OvertimeRequestDto> requests = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Overtime request retrieved successfully", requests, PaginationMeta.of(page));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> updateOvertimeRequest(@PathVariable UUID id, @RequestBody @Valid UpdateOvertimeRequest request) {
        OvertimeRequest entity = service.updateOvertimeRequest(id, request);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request updated successfully", dto);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('HR', 'SUPERVISOR')")
    public ResponseEntity<ApiResponse<OvertimeRequestDto>> updateOvertimeRequestStatus(@PathVariable UUID id, @RequestParam RequestStatus status) {
        OvertimeRequest entity = service.updateOvertimeRequestStatus(id, status);
        OvertimeRequestDto dto = mapper.toDto(entity);

        return ResponseFactory.ok("Overtime request status updated successfully", dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteOvertimeRequest(@PathVariable UUID id) {
        service.deleteOvertimeRequest(id);

        DeleteResponse res = DeleteResponse.builder()
                .resourceType("Overtime Request")
                .resourceId(id)
                .build();

        return ResponseFactory.ok("Overtime request deleted successfully", res);
    }

}
