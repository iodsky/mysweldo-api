package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.payroll.item.PayrollItemDto;
import com.iodsky.mysweldo.payroll.item.UpdatePayrollBenefitRequest;
import com.iodsky.mysweldo.payroll.item.UpdatePayrollDeductionRequest;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@Tag(name = "Payroll Runs", description = "Payroll run processing and management endpoints")
public class PayrollRunController {

    private final PayrollRunService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create payroll run", description = "Create a new payroll run", operationId = "createPayrollRun")
    public PayrollRunDto createPayrollRun(@Valid @RequestBody PayrollRunRequest request) {
        PayrollRunDto dto = service.createPayrollRun(request);
        return dto;
    }

    @PostMapping("/{id}/generate")
    @Operation(summary = "Generate payroll", description = "Generate payroll items for a specific payroll run", operationId = "generatePayroll")
    public GeneratePayrollResponse generatePayroll(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Valid @RequestBody GeneratePayrollRequest request) {
        GeneratePayrollResponse response = service.generatePayroll(id, request);
        return response;
    }

    @GetMapping
    @Operation(summary = "Get all payroll runs", description = "Retrieve a paginated list of payroll runs with optional filters", operationId = "getAllPayrollRuns")
    public PageDto<PayrollRunDto> getAllPayrollRuns(
            @Parameter(description = "Filter by period start date") @RequestParam(required = false) LocalDate periodStartDate,
            @Parameter(description = "Filter by period end date") @RequestParam(required = false) LocalDate periodEndDate,
            @Parameter(description = "Filter by payroll run type") @RequestParam(required = false) PayrollRunType type,
            @Parameter(description = "Filter by payroll run status") @RequestParam(required = false) PayrollRunStatus status,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<PayrollRunDto> page = service.getAllPayrollRuns(periodStartDate, periodEndDate, type, status, pageNo, limit);
        List<PayrollRunDto> runs = page.getContent();
        return PageDto.of(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payroll run by ID", description = "Retrieve a specific payroll run by its ID", operationId = "getPayrollRunById")
    public PayrollRunDto getPayrollRunById(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id) {
        PayrollRunDto run = service.getPayrollRunById(id);
        return run;
    }

    @GetMapping("/{id}/items")
    @Operation(summary = "Get payroll items", description = "Retrieve a paginated list of payroll items for a specific payroll run", operationId = "getPayrollItems")
    public PageDto<PayrollItemDto> getAllPayrollItems(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<PayrollItemDto> page = service.getPayrollItems(id, pageNo, limit);
        List<PayrollItemDto> items = page.getContent();
        return PageDto.of(page);
    }

    @GetMapping("/{id}/items/{itemId}")
    @Operation(summary = "Get payroll item", description = "Retrieve a specific payroll item by its ID", operationId = "getPayrollItem")
    public PayrollItemDto getPayrollItem(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "Payroll item ID") @PathVariable UUID itemId) {
        PayrollItemDto item = service.getPayrollItem(id, itemId);
        return item;
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update payroll run status", description = "Update the status of a specific payroll run", operationId = "updatePayrollRunStatus")
    public PayrollRunDto updatePayrollRunStatus(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "New payroll run status") @RequestParam PayrollRunStatus status) {
        PayrollRunDto run = service.updatePayrollRunStatus(id, status);
        return run;
    }

    @PatchMapping("/{id}/items/{itemId}/deductions")
    @Operation(summary = "Update payroll deductions", description = "Update deductions for a specific payroll item", operationId = "updatePayrollDeductions")
    public PayrollItemDto updatePayrollDeductions(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "Payroll item ID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdatePayrollDeductionRequest request
    ) {
        PayrollItemDto payrollItemDto = service.updatePayrollDeductions(id, itemId, request);
        return payrollItemDto;
    }

    @PatchMapping("/{id}/items/{itemId}/benefits")
    @Operation(summary = "Update payroll benefits", description = "Update benefits for a specific payroll item", operationId = "updatePayrollBenefits")
    public PayrollItemDto updatePayrollBenefits(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "Payroll item ID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdatePayrollBenefitRequest request
    ) {
        PayrollItemDto payrollItemDto = service.updatePayrollBenefits(id, itemId, request);
        return payrollItemDto;
    }

    @DeleteMapping("/{id}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete payroll item", description = "Delete a specific payroll item from a payroll run", operationId = "deletePayrollItem")
    public void deletePayrollItem(
            @Parameter(description = "Payroll run ID") @PathVariable UUID id,
            @Parameter(description = "Payroll item ID") @PathVariable UUID itemId) {
        service.deletePayrollItem(id, itemId);

        
    }

}
