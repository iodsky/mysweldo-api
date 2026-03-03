package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.PaginationMeta;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payrolls")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Payroll processing and management endpoints")
public class PayrollController {

    private final PayrollService service;
    private final PayrollMapper mapper;

    @PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
    @PostMapping
    @Operation(summary = "Create payroll", description = "Generate payroll for a single employee. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PayrollDto>> createPayroll(@RequestBody CreatePayrollRequest request) {
        Payroll entity = service.createPayroll(
                request.getEmployeeId(),
                request.getPeriodStartDate(),
                request.getPeriodEndDate(),
                request.getPayDate());
        PayrollDto dto = mapper.toDto(entity);

        return ResponseFactory.created("Payroll created successfully", dto);
    }

    @PreAuthorize("hasAnyRole('PAYROLL', 'HR', 'SUPERUSER')")
    @GetMapping
    @Operation(summary = "Get all payroll records", description = "Retrieve all payroll records with pagination and optional date filtering. Requires PAYROLL or HR role.")
    public ResponseEntity<ApiResponse<List<PayrollDto>>> getAllPayroll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by year and month") @RequestParam(required = false) YearMonth period
            ) {

        Page<Payroll> page = service.getAllPayroll(pageNo, limit, period);

        List<PayrollDto> payroll = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Payroll retrieved successfully", payroll, PaginationMeta.of(page));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my payroll records", description = "Retrieve payroll records for the authenticated employee")
    public ResponseEntity<ApiResponse<List<PayrollDto>>> getAllEmployeePayroll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by year and month") @RequestParam(required = false) YearMonth period
    ) {
        Page<Payroll> page = service.getAllEmployeePayroll(pageNo, limit, period);

        List<PayrollDto> payroll = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Payroll retrieved successfully", payroll, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payroll by ID", description = "Retrieve a specific payroll record by its ID")
    public ResponseEntity<ApiResponse<PayrollDto>> getPayrollById(@Parameter(description = "Payroll ID") @PathVariable("id") UUID id) {
        PayrollDto dto = mapper.toDto(service.getPayrollById(id));
        return ResponseFactory.ok("Payroll retrieved successfully", dto);
    }
}
