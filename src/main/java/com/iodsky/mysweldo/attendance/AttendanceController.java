package com.iodsky.mysweldo.attendance;

import com.iodsky.mysweldo.common.response.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
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
    @Operation(summary = "Create attendance record", description = "Create a new attendance record. Requires HR role.")
    public ApiResponse<AttendanceDto> createAttendance(@Valid @RequestBody AttendanceRequest request) {
        AttendanceDto dto = service.createAttendance(request);
        return ResponseFactory.success("Attendance created successfully", dto);
    }

    @PostMapping("/clock-in")
    @Operation(summary = "Clock in", description = "Record clock in time for the authenticated employee")
    public ApiResponse<AttendanceDto> clockIn() {
        AttendanceDto attendanceDto = service.clockIn();
        return ResponseFactory.success("You have successfully clocked in for the day", attendanceDto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Get all attendances", description = "Retrieve all attendance records with pagination and optional date filtering. Requires HR role.")
    public ApiResponse<List<AttendanceDto>> getAllAttendances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getAllAttendances(pageNo, limit, startDate, endDate);
        List<AttendanceDto> data = page.getContent();

        return ResponseFactory.success("Attendances retrieved successfully", data, PaginationMeta.of(page));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my attendances", description = "Retrieve attendance records for the authenticated employee")
    public ApiResponse<List<AttendanceDto>> getMyAttendances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getEmployeeAttendances(pageNo, limit, null, startDate, endDate);
        List<AttendanceDto> data = page.getContent();

        return ResponseFactory.success("Attendances retrieved successfully", data, PaginationMeta.of(page));
    }

    @GetMapping("/employee/{id}")
    @PreAuthorize("hasAnyRole('HR', 'PAYROLL', 'SUPERUSER')")
    @Operation(summary = "Get employee attendances", description = "Retrieve attendance records for a specific employee. Requires HR or Payroll role.")
    public ApiResponse<List<AttendanceDto>> getEmployeeAttendancesForHR(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<AttendanceDto> page = service.getEmployeeAttendances(pageNo, limit, id, startDate, endDate);
        List<AttendanceDto> data = page.getContent();

        return ResponseFactory.success("Attendances retrieved successfully", data, PaginationMeta.of(page));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @Operation(summary = "Update attendance", description = "Update an existing attendance record. Requires HR role.")
    public ApiResponse<AttendanceDto> updateAttendance(@Parameter(description = "Attendance ID") @PathVariable UUID id, @Valid @RequestBody AttendanceRequest request) {
        AttendanceDto dto = service.updateAttendance(id, request);
        return ResponseFactory.success("Attendance updated successfully", dto);
    }

    @PatchMapping("/clock-out")
    @Operation(summary = "Clock out", description = "Record clock out time for the authenticated employee")
    public ApiResponse<AttendanceDto> clockOut() {
        AttendanceDto attendanceDto = service.clockOut();
        return ResponseFactory.success("You successfully have clocked out for the day", attendanceDto);
    }
}
