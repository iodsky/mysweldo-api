package com.iodsky.mysweldo.payroll.contribution.philhealth;

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
@RequestMapping("/payroll-config/philhealth")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - PhilHealth", description = "Manage PhilHealth rate table configurations")
public class PhilhealthRateTableController {

    private final PhilhealthRateTableService service;
    private final PhilhealthRateTableMapper mapper;

    @PostMapping
    @Operation(summary = "Create PhilHealth rate table", description = "Create a new PhilHealth rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthRateTableDto>> createPhilhealthRateTable(
            @Valid @RequestBody PhilhealthRateTableRequest request) {
        PhilhealthRateTable rateTable = service.createPhilhealthRateTable(request);
        return ResponseFactory.created(
                "PhilHealth rate table created successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping
    @Operation(summary = "Get all PhilHealth rate tables", description = "Retrieve all PhilHealth rate tables with pagination. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<PhilhealthRateTableDto>>> getAllPhilhealthRateTables(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date (on or before)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<PhilhealthRateTable> page = service.getAllPhilhealthRateTables(pageNo, limit, effectiveDate);
        List<PhilhealthRateTableDto> rateTables = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "PhilHealth rate tables retrieved successfully",
                rateTables,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PhilHealth rate table by ID", description = "Retrieve a specific PhilHealth rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthRateTableDto>> getPhilhealthRateTableById(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        PhilhealthRateTable rateTable = service.getPhilhealthRateTableById(id);
        return ResponseFactory.ok(
                "PhilHealth rate table retrieved successfully",
                mapper.toDto(rateTable)
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest PhilHealth rate table", description = "Retrieve the latest PhilHealth rate table for a given date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthRateTableDto>> getLatestPhilhealthRateTable(
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        PhilhealthRateTable rateTable = service.getLatestPhilhealthRateTable(effectiveDate);
        return ResponseFactory.ok(
                "Latest PhilHealth rate table retrieved successfully",
                mapper.toDto(rateTable)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update PhilHealth rate table", description = "Update an existing PhilHealth rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthRateTableDto>> updatePhilhealthRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id,
            @Valid @RequestBody PhilhealthRateTableRequest request) {
        PhilhealthRateTable rateTable = service.updatePhilhealthRateTable(id, request);
        return ResponseFactory.ok(
                "PhilHealth rate table updated successfully",
                mapper.toDto(rateTable)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete PhilHealth rate table", description = "Soft delete a PhilHealth rate table. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePhilhealthRateTable(
            @Parameter(description = "Rate table ID") @PathVariable UUID id) {
        service.deletePhilhealthRateTable(id);
        return ResponseFactory.ok(
                "PhilHealth rate table deleted successfully",
                new DeleteResponse("PhilHealthRateTable", id)
        );
    }
}
