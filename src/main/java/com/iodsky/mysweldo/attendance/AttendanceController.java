package com.iodsky.mysweldo.attendance;

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

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/attendances")
@RequiredArgsConstructor
@Tag(name = "Attendance", description = "Attendance tracking and management endpoints")
public class AttendanceController {

    private final AttendanceService service;

    @PostMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create attendance record", description = "Create a new attendance record. Requires HR role.", operationId = "createAttendance")
    public AttendanceDto createAttendance(@Valid @RequestBody AttendanceRequest request) {
        return service.createAttendance(request);
    }

    @PostMapping("/clock-in")
    @Operation(summary = "Clock in", description = "Record clock in time for the authenticated employee", operationId = "clockIn")
    public AttendanceDto clockIn() {
        return service.clockIn();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get all attendances", description = "Retrieve all attendance records with pagination and optional date filtering. Requires HR role.", operationId = "getAllAttendances")
    public PageDto<AttendanceDto> getAllAttendances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getAllAttendances(pageNo, limit, startDate, endDate);
        return PageDto.of(page);
    }

    @GetMapping("/me")
    @Operation(summary = "Get my attendances", description = "Retrieve attendance records for the authenticated employee", operationId = "getMyAttendances")
    public PageDto<AttendanceDto> getMyAttendances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getEmployeeAttendances(pageNo, limit, null, startDate, endDate);
        return PageDto.of(page);
    }

    @GetMapping("/employee/{id}")
    @PreAuthorize("hasAnyRole('HR', 'PAYROLL', 'SUPERUSER')")
    @Operation(summary = "Get employee attendances", description = "Retrieve attendance records for a specific employee. Requires HR or Payroll role.", operationId = "getEmployeeAttendances")
    public PageDto<AttendanceDto> getEmployeeAttendancesForHR(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getEmployeeAttendances(pageNo, limit, id, startDate, endDate);
        return PageDto.of(page);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Update attendance", description = "Update an existing attendance record. Requires HR role.", operationId = "updateAttendance")
    public AttendanceDto updateAttendance(@Parameter(description = "Attendance ID") @PathVariable UUID id, @Valid @RequestBody AttendanceRequest request) {
        return service.updateAttendance(id, request);
    }

    @PatchMapping("/clock-out")
    @Operation(summary = "Clock out", description = "Record clock out time for the authenticated employee", operationId = "clockOut")
    public AttendanceDto clockOut() {
        return service.clockOut();
    }
}
