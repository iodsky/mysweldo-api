package com.iodsky.mysweldo.philhealth;

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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/philhealth-rates")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - PhilHealth", description = "Manage PhilHealth rate configurations")
public class PhilhealthRateController {

    private final PhilhealthRateService service;
    private final PhilhealthRateMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create PhilHealth rate", description = "Create a new PhilHealth rate. Requires PAYROLL role.")
    public ApiResponse<PhilhealthRateDto> createPhilhealthRate(
            @Valid @RequestBody PhilhealthRateRequest request) {
        PhilhealthRate philhealthRate = service.createPhilhealthRate(request);
        return ResponseFactory.success(
                "PhilHealth rate created successfully",
                mapper.toDto(philhealthRate)
        );
    }

    @GetMapping
    @Operation(summary = "Get all PhilHealth rates", description = "Retrieve all PhilHealth rates with pagination. Requires PAYROLL role.")
    public ApiResponse<List<PhilhealthRateDto>> getAllPhilhealthRates(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date (on or before)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<PhilhealthRate> page = service.getAllPhilhealthRates(pageNo, limit, effectiveDate);
        List<PhilhealthRateDto> philhealthRates = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "PhilHealth rates retrieved successfully",
                philhealthRates,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PhilHealth rate by ID", description = "Retrieve a specific PhilHealth rate. Requires PAYROLL role.")
    public ApiResponse<PhilhealthRateDto> getPhilhealthRateById(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        PhilhealthRate philhealthRate = service.getPhilhealthRateById(id);
        return ResponseFactory.success(
                "PhilHealth rate retrieved successfully",
                mapper.toDto(philhealthRate)
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest PhilHealth rate", description = "Retrieve the latest PhilHealth rate for a given date. Requires PAYROLL role.")
    public ApiResponse<PhilhealthRateDto> getLatestPhilhealthRate(
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        PhilhealthRate philhealthRate = service.getLatestPhilhealthRate(effectiveDate);
        return ResponseFactory.success(
                "Latest PhilHealth rate retrieved successfully",
                mapper.toDto(philhealthRate)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update PhilHealth rate", description = "Update an existing PhilHealth rate. Requires PAYROLL role.")
    public ApiResponse<PhilhealthRateDto> updatePhilhealthRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id,
            @Valid @RequestBody PhilhealthRateRequest request) {
        PhilhealthRate philhealthRate = service.updatePhilhealthRate(id, request);
        return ResponseFactory.success(
                "PhilHealth rate updated successfully",
                mapper.toDto(philhealthRate)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete PhilHealth rate", description = "Soft delete a PhilHealth rate. Requires PAYROLL role.")
    public ApiResponse<Void> deletePhilhealthRate(
            @Parameter(description = "Rate ID") @PathVariable UUID id) {
        service.deletePhilhealthRate(id);
        return ResponseFactory.success("PhilHealth rate deleted successfully");
    }
}
