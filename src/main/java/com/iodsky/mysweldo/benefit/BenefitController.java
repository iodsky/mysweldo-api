package com.iodsky.mysweldo.benefit;

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

import java.util.List;

@RestController
@RequestMapping("/benefits")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PAYROLL', 'SUPERUSER')")
@Tag(name = "Benefits", description = "Manage benefit configurations")
public class BenefitController {

    private final BenefitService service;
    private final BenefitMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create benefit", description = "Create a new benefit. Requires PAYROLL role.")
    public ApiResponse<BenefitDto> createBenefitType(@Valid @RequestBody BenefitRequest request) {
        Benefit benefit = service.createBenefit(request);
        return ResponseFactory.success("Benefit created successfully", mapper.toDto(benefit));
    }

    @GetMapping
    @Operation(summary = "Get all benefit", description = "Retrieve all benefit with pagination. Requires PAYROLL or HR role.")
    public ApiResponse<List<BenefitDto>> getAllBenefitTypes(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        Page<Benefit> page = service.getAllBenefits(pageNo, limit);
        List<BenefitDto> benefits = page.getContent().stream().map(mapper::toDto).toList();
        return ResponseFactory.success("Benefit retrieved successfully", benefits, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get benefit type by code", description = "Retrieve a specific benefit. Requires PAYROLL or HR role.")
    public ApiResponse<BenefitDto> getBenefitTypeById(
            @Parameter(description = "Benefit code") @PathVariable String id) {
        Benefit benefit = service.getBenefitByCode(id);
        return ResponseFactory.success("Benefit retrieved successfully", mapper.toDto(benefit));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update benefit", description = "Update an existing benefit. Requires PAYROLL role.")
    public ApiResponse<BenefitDto> updateBenefitType(
            @Parameter(description = "Benefit code") @PathVariable String id,
            @Valid @RequestBody BenefitRequest request) {
        Benefit benefit = service.updateBenefit(id, request);
        return ResponseFactory.success("Benefit updated successfully", mapper.toDto(benefit));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete benefit", description = "Soft delete a benefit. Requires PAYROLL role.")
    public ApiResponse<Void> deleteBenefitType(
            @Parameter(description = "Benefit code") @PathVariable String id) {
        service.deleteBenefit(id);
        return ResponseFactory.success("Benefit deleted successfully");
    }
}
