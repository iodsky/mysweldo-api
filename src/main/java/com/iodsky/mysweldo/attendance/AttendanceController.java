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
    private final AttendanceMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create attendance record", description = "Create a new attendance record for the authenticated employee")
    public ApiResponse<AttendanceDto> createAttendance(@Valid @RequestBody(required = false) AttendanceDto attendanceDto) {
        Attendance attendance = service.createAttendance(attendanceDto);
        AttendanceDto dto = mapper.toDto(attendance);
        return ResponseFactory.success("Attendance created successfully", dto);
    }

    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @GetMapping
    @Operation(summary = "Get all attendances", description = "Retrieve all attendance records with pagination and optional date filtering. Requires HR role.")
    public ApiResponse<List<AttendanceDto>> getAllAttendances(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<Attendance> page = service.getAllAttendances(pageNo, limit, startDate, endDate);

        List<AttendanceDto> data = page.getContent().stream().map(mapper::toDto).toList();

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
        Page<Attendance> page = service
                .getEmployeeAttendances(pageNo, limit, null, startDate, endDate);

        List<AttendanceDto> data = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.success("Attendances retrieved successfully", data, PaginationMeta.of(page));
    }

    @PreAuthorize("hasAnyRole('HR', 'PAYROLL', 'SUPERUSER')")
    @GetMapping("/employee/{id}")
    @Operation(summary = "Get employee attendances", description = "Retrieve attendance records for a specific employee. Requires HR or Payroll role.")
    public ApiResponse<List<AttendanceDto>> getEmployeeAttendancesForHR(
            @Parameter(description = "Employee ID") @PathVariable Long id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by start date") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "Filter by end date") @RequestParam(required = false) LocalDate endDate
    ) {
        Page<Attendance> page = service
                .getEmployeeAttendances(pageNo, limit, id, startDate, endDate);

        List<AttendanceDto> data = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.success("Attendances retrieved successfully", data,  PaginationMeta.of(page));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update attendance", description = "Update an existing attendance record")
    public ApiResponse<AttendanceDto> updateAttendance(@Parameter(description = "Attendance ID") @PathVariable UUID id, @Valid @RequestBody(required = false) AttendanceDto attendanceDto) {
        Attendance attendance = service.updateAttendance(id, attendanceDto);
        AttendanceDto dto = mapper.toDto(attendance);
        return ResponseFactory.success("Attendance updated successfully", dto);
    }
}
