package com.iodsky.mysweldo.payroll.contribution.sss;

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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll-config/sss")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - SSS", description = "Manage SSS rate table configurations")
public class SssRateTableController {

    private final SssRateTableService service;
    private final SssRateTableMapper mapper;

    @PostMapping
    @Operation(summary = "Create SSS rate table", description = "Create a new SSS rate table with salary brackets. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<SssRateTableDto>> createSssRateTable(
            @Valid @RequestBody SssRateTableRequest request) {
        SssRateTable rateTable = service.createSssRateTable(request);
        return ResponseFactory.created(
                "SSS rate table created successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping
    @Operation(summary = "Get all SSS rate tables", description = "Retrieve all SSS rate tables with pagination and filters. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<SssRateTableDto>>> getAllSssRateTables(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<SssRateTable> page = service.getAllSssRateTables(
                pageNo, limit, effectiveDate);
        List<SssRateTableDto> rateTables = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "SSS rate tables retrieved successfully",
                rateTables,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get SSS rate table by ID", description = "Retrieve a specific SSS rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<SssRateTableDto>> getSssRateTableById(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        SssRateTable rateTable = service.getSssRateTableById(id);
        return ResponseFactory.ok(
                "SSS rate table retrieved successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup SSS rate table by salary", description = "Find the SSS rate table for a given salary and date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<SssRateTableDto>> getSssRateTableBySalary(
            @Parameter(description = "Salary amount") @RequestParam BigDecimal salary,
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        SssRateTable rateTable = service.getSssRateTableBySalaryAndDate(salary, effectiveDate);
        return ResponseFactory.ok(
                "SSS rate table found for salary",
                mapper.toDto(rateTable)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update SSS rate table", description = "Update an existing SSS rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<SssRateTableDto>> updateSssRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id,
            @Valid @RequestBody SssRateTableRequest request) {
        SssRateTable rateTable = service.updateSssRateTable(id, request);
        return ResponseFactory.ok(
                "SSS rate table updated successfully",
                mapper.toDto(rateTable)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete SSS rate table", description = "Soft delete an SSS rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteSssRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        service.deleteSssRateTable(id);
        return ResponseFactory.ok(
                "SSS rate table deleted successfully",
                new DeleteResponse("SssRateTable", id.toString())
        );
    }
}
