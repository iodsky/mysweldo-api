package com.iodsky.sweldox.benefit;

import com.iodsky.sweldox.common.response.ApiResponse;
import com.iodsky.sweldox.common.response.DeleteResponse;
import com.iodsky.sweldox.common.response.PaginationMeta;
import com.iodsky.sweldox.common.response.ResponseFactory;
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

import java.util.List;

@RestController
@RequestMapping("/benefit-types")
@Validated
@RequiredArgsConstructor
@Tag(name = "Benefit Types", description = "Manage benefit type configurations")
public class BenefitTypeController {

    private final BenefitTypeService benefitTypeService;
    private final BenefitTypeMapper benefitTypeMapper;

    @PreAuthorize("hasRole('PAYROLL')")
    @PostMapping
    @Operation(summary = "Create benefit type", description = "Create a new benefit type. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<BenefitTypeDto>> createBenefitType(
            @Valid @RequestBody BenefitTypeRequest request) {
        BenefitType benefitType = benefitTypeService.createBenefitType(request);
        return ResponseFactory.created(
                "Benefit type created successfully",
                benefitTypeMapper.toDto(benefitType)
        );
    }

    @PreAuthorize("hasAnyRole('PAYROLL', 'HR')")
    @GetMapping
    @Operation(summary = "Get all benefit types", description = "Retrieve all benefit types with pagination. Requires PAYROLL or HR role.")
    public ResponseEntity<ApiResponse<List<BenefitTypeDto>>> getAllBenefitTypes(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        Page<BenefitType> page = benefitTypeService.getAllBenefitTypes(pageNo, limit);
        List<BenefitTypeDto> benefitTypes = page.getContent().stream()
                .map(benefitTypeMapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "Benefit types retrieved successfully",
                benefitTypes,
                PaginationMeta.of(page)
        );
    }

    @PreAuthorize("hasAnyRole('PAYROLL', 'HR')")
    @GetMapping("/{id}")
    @Operation(summary = "Get benefit type by ID", description = "Retrieve a specific benefit type. Requires PAYROLL or HR role.")
    public ResponseEntity<ApiResponse<BenefitTypeDto>> getBenefitTypeById(
            @Parameter(description = "Benefit type ID") @PathVariable String id) {
        BenefitType benefitType = benefitTypeService.getBenefitTypeById(id);
        return ResponseFactory.ok(
                "Benefit type retrieved successfully",
                benefitTypeMapper.toDto(benefitType)
        );
    }

    @PreAuthorize("hasRole('PAYROLL')")
    @PutMapping("/{id}")
    @Operation(summary = "Update benefit type", description = "Update an existing benefit type. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<BenefitTypeDto>> updateBenefitType(
            @Parameter(description = "Benefit type ID") @PathVariable String id,
            @Valid @RequestBody BenefitTypeRequest request) {
        BenefitType benefitType = benefitTypeService.updateBenefitType(id, request);
        return ResponseFactory.ok(
                "Benefit type updated successfully",
                benefitTypeMapper.toDto(benefitType)
        );
    }

    @PreAuthorize("hasRole('PAYROLL')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete benefit type", description = "Soft delete a benefit type. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteBenefitType(
            @Parameter(description = "Benefit type ID") @PathVariable String id) {
        benefitTypeService.deleteBenefitType(id);
        return ResponseFactory.ok(
                "Benefit type deleted successfully",
                new DeleteResponse("BenefitType", id)
        );
    }
}
