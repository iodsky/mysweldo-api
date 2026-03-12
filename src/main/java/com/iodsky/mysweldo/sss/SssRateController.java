package com.iodsky.mysweldo.sss;

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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/sss-rates")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - SSS", description = "Manage SSS rate configurations")
public class SssRateController {

    private final SssRateService service;
    private final SssRateMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create SSS rate", description = "Create a new SSS rate with salary brackets. Requires PAYROLL role.")
    public ApiResponse<SssRateDto> createSssRate(
            @Valid @RequestBody SssRateRequest request) {
        SssRate sssRate = service.createSssRate(request);
        return ResponseFactory.success(
                "SSS rate created successfully",
                mapper.toDto(sssRate)
        );
    }

    @GetMapping
    @Operation(summary = "Get all SSS rates", description = "Retrieve all SSS rates with pagination and filters. Requires PAYROLL role.")
    public ApiResponse<List<SssRateDto>> getAllSssRate(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<SssRate> page = service.getAllSssRate(
                pageNo, limit, effectiveDate);
        List<SssRateDto> sssRates = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "SSS rates retrieved successfully",
                sssRates,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get SSS rate by ID", description = "Retrieve a specific SSS rate. Requires PAYROLL role.")
    public ApiResponse<SssRateDto> getSssRateById(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        SssRate sssRate = service.getSssRateById(id);
        return ResponseFactory.success(
                "SSS rate retrieved successfully",
                mapper.toDto(sssRate)
        );
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup SSS rate by salary", description = "Find the SSS rate for a given salary and date. Requires PAYROLL role.")
    public ApiResponse<SssRateDto> getSssRateBySalary(
            @Parameter(description = "Salary amount") @RequestParam BigDecimal salary,
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        SssRate sssRate = service.getSssRateBySalaryAndDate(salary, effectiveDate);
        return ResponseFactory.success(
                "SSS rate found for salary",
                mapper.toDto(sssRate)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update SSS rate", description = "Update an existing SSS rate. Requires PAYROLL role.")
    public ApiResponse<SssRateDto> updateSssRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id,
            @Valid @RequestBody SssRateRequest request) {
        SssRate sssRate = service.updateSssRate(id, request);
        return ResponseFactory.success(
                "SSS rate updated successfully",
                mapper.toDto(sssRate)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete SSS rate", description = "Soft delete an SSS rate. Requires PAYROLL role.")
    public ApiResponse<Void> deleteSssRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        service.deleteSssRate(id);
        return ResponseFactory.success("SSS rate deleted successfully");
    }
}
