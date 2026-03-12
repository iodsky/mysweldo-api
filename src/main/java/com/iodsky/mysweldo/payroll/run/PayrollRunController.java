package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.DeleteResponse;
import com.iodsky.mysweldo.common.response.PaginationMeta;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import com.iodsky.mysweldo.payroll.core.PayrollItemDto;
import com.iodsky.mysweldo.payroll.core.UpdatePayrollBenefitRequest;
import com.iodsky.mysweldo.payroll.core.UpdatePayrollContributionRequest;
import com.iodsky.mysweldo.payroll.core.UpdatePayrollDeductionRequest;
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
@RequestMapping("/payroll-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
public class PayrollRunController {

    private final PayrollRunService service;

    @PostMapping
    public ResponseEntity<ApiResponse<PayrollRunDto>> createPayrollRun(@Valid @RequestBody PayrollRunRequest request) {
        PayrollRunDto dto = service.createPayrollRun(request);
        return ResponseFactory.created("PayrollRun successfully created", dto);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<GeneratePayrollResponse>> generatePayroll(@PathVariable UUID id, @Valid @RequestBody GeneratePayrollRequest request) {
        GeneratePayrollResponse response = service.generatePayroll(id, request);
        return ResponseFactory.ok("Payroll generated successfully", response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PayrollRunDto>>> getAllPayrollRuns(
            @RequestParam(required = false) LocalDate periodStartDate,
            @RequestParam(required = false) LocalDate periodEndDate,
            @RequestParam(required = false) PayrollRunType type,
            @RequestParam(required = false) PayrollRunStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<PayrollRunDto> page = service.getAllPayrollRuns(periodStartDate, periodEndDate, type, status, pageNo, limit);
        List<PayrollRunDto> runs = page.getContent();
        return ResponseFactory.ok("Payroll runs retrieved successfully", runs, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PayrollRunDto>> getPayrollRunById(@PathVariable UUID id) {
        PayrollRunDto run = service.getPayrollRunById(id);
        return ResponseFactory.ok("Payroll run successfully retrieved", run);
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<ApiResponse<List<PayrollItemDto>>> getAllPayrollItems(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit
    ) {
        Page<PayrollItemDto> page = service.getPayrollItems(id, pageNo, limit);
        List<PayrollItemDto> items = page.getContent();
        return ResponseFactory.ok("Payroll runs retrieved successfully", items, PaginationMeta.of(page));
    }

    @GetMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<PayrollItemDto>> getPayrollItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        PayrollItemDto item = service.getPayrollItem(id, itemId);
        return ResponseFactory.ok("Payroll item retrieved successfully", item);
    }

    @PatchMapping("/{id}/items/{itemId}/deductions")
    public ResponseEntity<ApiResponse<PayrollItemDto>> updatePayrollDeductions(@PathVariable UUID id,
                                                                                   @PathVariable UUID itemId,
                                                                                   @Valid @RequestBody UpdatePayrollDeductionRequest request
    ) {
        PayrollItemDto payrollItemDto = service.updatePayrollDeductions(id, itemId, request);
        return ResponseFactory.ok("Payroll updated successfully", payrollItemDto);
    }

    @PatchMapping("/{id}/items/{itemId}/benefits")
    public ResponseEntity<ApiResponse<PayrollItemDto>> updatePayrollBenefits(@PathVariable UUID id,
                                                                                   @PathVariable UUID itemId,
                                                                                   @Valid @RequestBody UpdatePayrollBenefitRequest request
    ) {
        PayrollItemDto payrollItemDto = service.updatePayrollBenefits(id, itemId, request);
        return ResponseFactory.ok("Payroll updated successfully", payrollItemDto);
    }

    @PatchMapping("/{id}/items/{itemId}/contributions")
    public ResponseEntity<ApiResponse<PayrollItemDto>> updatePayrollContributions(@PathVariable UUID id,
                                                                                 @PathVariable UUID itemId,
                                                                                 @Valid @RequestBody UpdatePayrollContributionRequest request
    ) {
        PayrollItemDto payrollItemDto = service.updatePayrollContributions(id, itemId, request);
        return ResponseFactory.ok("Payroll updated successfully", payrollItemDto);
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePayrollItem(@PathVariable UUID id, @PathVariable UUID itemId) {
        service.deletePayrollItem(id, itemId);

        DeleteResponse response = DeleteResponse.builder()
                .resourceType("PayrollItem")
                .resourceId(itemId)
                .build();

        return ResponseFactory.ok("Payroll item retrieved successfully", response);
    }

}
