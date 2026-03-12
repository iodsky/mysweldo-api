package com.iodsky.mysweldo.pagIbig;

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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pagibig-rates")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - Pag-IBIG", description = "Manage Pag-IBIG rate configurations")
public class PagibigRateController {

    private final PagibigRateService service;
    private final PagibigRateMapper mapper;

    @PostMapping
    @Operation(summary = "Create Pag-IBIG rate", description = "Create a new Pag-IBIG rate. Requires PAYROLL role.")
    public ApiResponse<PagibigRateDto> createPagibigRate(
            @Valid @RequestBody PagibigRateRequest request) {
        PagibigRate pagibigRate = service.createPagibigRate(request);
        return ResponseFactory.success(
                "Pag-IBIG rate created successfully",
                mapper.toDto(pagibigRate)
        );
    }

    @GetMapping
    @Operation(summary = "Get all Pag-IBIG rates", description = "Retrieve all Pag-IBIG rates with pagination. Requires PAYROLL role.")
    public ApiResponse<List<PagibigRateDto>> getAllPagibigRates(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date (on or before)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<PagibigRate> page = service.getAllPagibigRates(pageNo, limit, effectiveDate);
        List<PagibigRateDto> pagibigRates = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "Pag-IBIG rates retrieved successfully",
                pagibigRates,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Pag-IBIG rate by ID", description = "Retrieve a specific Pag-IBIG rate. Requires PAYROLL role.")
    public ApiResponse<PagibigRateDto> getPagibigRateById(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        PagibigRate pagibigRate = service.getPagibigRateById(id);
        return ResponseFactory.success(
                "Pag-IBIG rate retrieved successfully",
                mapper.toDto(pagibigRate)
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest Pag-IBIG rate", description = "Retrieve the latest Pag-IBIG rate for a given date. Requires PAYROLL role.")
    public ApiResponse<PagibigRateDto> getLatestPagibigRate(
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        PagibigRate pagibiRate = service.getLatestPagibigRate(effectiveDate);
        return ResponseFactory.success(
                "Latest Pag-IBIG rate retrieved successfully",
                mapper.toDto(pagibiRate)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update Pag-IBIG rate", description = "Update an existing Pag-IBIG rate. Requires PAYROLL role.")
    public ApiResponse<PagibigRateDto> updatePagibigRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id,
            @Valid @RequestBody PagibigRateRequest request) {
        PagibigRate pagibigRate = service.updatePagibigRate(id, request);
        return ResponseFactory.success(
                "Pag-IBIG rate updated successfully",
                mapper.toDto(pagibigRate)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete Pag-IBIG rate", description = "Soft delete a Pag-IBIG rate. Requires PAYROLL role.")
    public ApiResponse<Void> deletePagibigRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        service.deletePagibigRate(id);
        return ResponseFactory.success("Pag-IBIG rate deleted successfully");
    }
}
