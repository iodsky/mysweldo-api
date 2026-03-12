package com.iodsky.mysweldo.contribution;

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
@RequestMapping("/contributions")
@Validated
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('HR', 'PAYROLL', 'SUPERUSER')")
@Tag(name = "Contributions", description = "Manage contribution configurations")
public class ContributionController {

    private final ContributionService service;
    private final ContributionMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create contribution", description = "Create a new contribution. Requires PAYROLL role.")
    public ApiResponse<ContributionDto> createContribution(@Valid @RequestBody ContributionRequest request) {
        Contribution contribution = service.createContribution(request);
        return ResponseFactory.success("Contribution created successfully", mapper.toDto(contribution));
    }

    @GetMapping
    @Operation(summary = "Get all contributions", description = "Retrieve all contributions with pagination. Requires PAYROLL or HR role.")
    public ApiResponse<List<ContributionDto>> getAllContributions(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        Page<Contribution> page = service.getAllContributions(pageNo, limit);
        List<ContributionDto> contributions = page.getContent().stream().map(mapper::toDto).toList();
        return ResponseFactory.success("Contributions retrieved successfully", contributions, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get contribution by code", description = "Retrieve a specific contribution. Requires PAYROLL or HR role.")
    public ApiResponse<ContributionDto> getContributionById(
            @Parameter(description = "Contribution code") @PathVariable String id) {
        Contribution contribution = service.getContributionByCode(id);
        return ResponseFactory.success("Contribution retrieved successfully", mapper.toDto(contribution));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update contribution", description = "Update an existing contribution. Requires PAYROLL role.")
    public ApiResponse<ContributionDto> updateContribution(
            @Parameter(description = "Contribution code") @PathVariable String id,
            @Valid @RequestBody ContributionRequest request) {
        Contribution contribution = service.updateContribution(id, request);
        return ResponseFactory.success("Contribution updated successfully", mapper.toDto(contribution));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contribution", description = "Soft delete a contribution. Requires PAYROLL role.")
    public ApiResponse<Void> deleteContribution(
            @Parameter(description = "Contribution code") @PathVariable String id) {
        service.deleteContribution(id);
        return ResponseFactory.success("Contribution deleted successfully");
    }
}
