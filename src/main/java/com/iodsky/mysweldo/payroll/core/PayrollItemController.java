package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.PaginationMeta;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/payrolls")
@RequiredArgsConstructor
@Tag(name = "Payroll", description = "Payroll processing and management endpoints")
public class PayrollItemController {

    private final PayrollItemService service;
    private final PayrollItemMapper mapper;

    @GetMapping("/me")
    @Operation(summary = "Get my payroll records", description = "Retrieve payroll records for the authenticated employee")
    public ResponseEntity<ApiResponse<List<PayrollItemDto>>> getAllEmployeePayroll(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by year and month") @RequestParam(required = false) YearMonth period
    ) {
        Page<PayrollItem> page = service.getAllEmployeePayroll(pageNo, limit, period);

        List<PayrollItemDto> payroll = page.getContent().stream().map(mapper::toDto).toList();

        return ResponseFactory.ok("Payroll retrieved successfully", payroll, PaginationMeta.of(page));
    }

}
