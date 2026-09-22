package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.common.response.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reports", description = "Generated report artifacts (payroll bank files, attendance timesheets)")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/payroll/{runId}")
    @PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate payroll bank file", description = "Generate and store a payroll bank file for a payroll run", operationId = "generatePayrollBankFile")
    public ReportDto generatePayrollBankFile(
            @Parameter(description = "Payroll run ID") @PathVariable UUID runId,
            @Parameter(description = "Report format") @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return reportService.createPayrollBankFile(runId, format);
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyRole('HR', 'SUPERUSER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Generate attendance timesheet", description = "Generate and store an attendance timesheet for a date range", operationId = "generateAttendanceTimesheet")
    public ReportDto generateAttendanceTimesheet(
            @Parameter(description = "Start date") @RequestParam LocalDate startDate,
            @Parameter(description = "End date") @RequestParam LocalDate endDate,
            @Parameter(description = "Report format") @RequestParam(defaultValue = "XLSX") ReportFormat format) {
        return reportService.createAttendanceTimesheet(startDate, endDate, format);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PAYROLL', 'HR', 'SUPERUSER')")
    @Operation(summary = "Get reports", description = "Retrieve a paginated list of report artifacts scoped to the authenticated role", operationId = "getAllReports")
    public PageDto<ReportDto> getAllReports(
            @Parameter(description = "Filter by report type") @RequestParam(required = false) ReportType type,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit) {
        Page<ReportDto> page = reportService.getAllReports(type, pageNo, limit);
        return PageDto.of(page);
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('PAYROLL', 'HR', 'SUPERUSER')")
    @Operation(summary = "Download report", description = "Download a generated report artifact", operationId = "downloadReport")
    public ResponseEntity<byte[]> downloadReport(@Parameter(description = "Report ID") @PathVariable UUID id) {
        ReportService.DownloadResult result = reportService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + result.fileName() + "\"")
                .contentType(MediaType.parseMediaType(result.contentType()))
                .body(result.bytes());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PAYROLL', 'HR', 'SUPERUSER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete report", description = "Delete a report artifact and its stored file", operationId = "deleteReport")
    public void deleteReport(@Parameter(description = "Report ID") @PathVariable UUID id) {
        reportService.delete(id);
    }

}