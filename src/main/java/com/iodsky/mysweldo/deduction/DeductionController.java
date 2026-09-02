package com.iodsky.mysweldo.deduction;

import com.iodsky.mysweldo.common.response.PageDto;
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
@RequestMapping("/deductions")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Deductions", description = "Manage deduction configurations")
public class DeductionController {

    private final DeductionService service;
    private final DeductionMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create deduction", description = "Create a new deduction. Requires PAYROLL role.", operationId = "createDeduction")
    public DeductionDto createDeduction(@Valid @RequestBody DeductionRequest request) {
        Deduction deduction = service.createDeduction(request);
        return mapper.toDto(deduction);
    }

    @GetMapping
    @Operation(summary = "Get all deductions", description = "Retrieve all deductions with pagination. Requires PAYROLL role.", operationId = "getAllDeductions")
    public PageDto<DeductionDto> getAllDeductions(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        Page<Deduction> page = service.getAllDeductions(pageNo, limit);
        List<DeductionDto> deductions = page.getContent().stream().map(mapper::toDto).toList();
        return PageDto.of(page.map(mapper::toDto));
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get deduction by code", description = "Retrieve a specific deduction. Requires PAYROLL role.", operationId = "getDeductionByCode")
    public DeductionDto getDeductionByCode(
            @Parameter(description = "Deduction code") @PathVariable String code) {
        Deduction deduction = service.getDeductionByCode(code);
        return mapper.toDto(deduction);
    }

    @PutMapping("/{code}")
    @Operation(summary = "Update deduction", description = "Update an existing deduction. Requires PAYROLL role.", operationId = "updateDeduction")
    public DeductionDto updateDeduction(
            @Parameter(description = "Deduction code") @PathVariable String code,
            @Valid @RequestBody DeductionRequest request) {
        Deduction deduction = service.updateDeduction(code, request);
        return mapper.toDto(deduction);
    }

    @DeleteMapping("/{code}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete deduction", description = "Soft delete a deduction. Requires PAYROLL role.", operationId = "deleteDeduction")
    public void deleteDeduction(
            @Parameter(description = "Deduction code") @PathVariable String code) {
        service.deleteDeduction(code);
        
    }
}
