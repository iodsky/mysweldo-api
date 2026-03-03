package com.iodsky.mysweldo.payroll.contribution.pagIbig;

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
@RequestMapping("/payroll-config/pagibig")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - Pag-IBIG", description = "Manage Pag-IBIG rate table configurations")
public class PagibigRateTableController {

    private final PagibigRateTableService service;
    private final PagibigRateTableMapper mapper;

    @PostMapping
    @Operation(summary = "Create Pag-IBIG rate table", description = "Create a new Pag-IBIG rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PagibigRateTableDto>> createPagibigRateTable(
            @Valid @RequestBody PagibigRateTableRequest request) {
        PagibigRateTable rateTable = service.createPagibigRateTable(request);
        return ResponseFactory.created(
                "Pag-IBIG rate table created successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping
    @Operation(summary = "Get all Pag-IBIG rate tables", description = "Retrieve all Pag-IBIG rate tables with pagination. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<PagibigRateTableDto>>> getAllPagibigRateTables(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date (on or before)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<PagibigRateTable> page = service.getAllPagibigRateTables(pageNo, limit, effectiveDate);
        List<PagibigRateTableDto> rateTables = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "Pag-IBIG rate tables retrieved successfully",
                rateTables,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Pag-IBIG rate table by ID", description = "Retrieve a specific Pag-IBIG rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PagibigRateTableDto>> getPagibigRateTableById(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        PagibigRateTable rateTable = service.getPagibigRateTableById(id);
        return ResponseFactory.ok(
                "Pag-IBIG rate table retrieved successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest Pag-IBIG rate table", description = "Retrieve the latest Pag-IBIG rate table for a given date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PagibigRateTableDto>> getLatestPagibigRateTable(
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        PagibigRateTable rateTable = service.getLatestPagibigRateTable(effectiveDate);
        return ResponseFactory.ok(
                "Latest Pag-IBIG rate table retrieved successfully",
                mapper.toDto(rateTable)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Pag-IBIG rate table", description = "Update an existing Pag-IBIG rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PagibigRateTableDto>> updatePagibigRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id,
            @Valid @RequestBody PagibigRateTableRequest request) {
        PagibigRateTable rateTable = service.updatePagibigRateTable(id, request);
        return ResponseFactory.ok(
                "Pag-IBIG rate table updated successfully",
                mapper.toDto(rateTable)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Pag-IBIG rate table", description = "Soft delete a Pag-IBIG rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePagibigRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        service.deletePagibigRateTable(id);
        return ResponseFactory.ok(
                "Pag-IBIG rate table deleted successfully",
                new DeleteResponse("PagibigRateTable", id)
        );
    }
}
