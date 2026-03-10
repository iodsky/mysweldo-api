package com.iodsky.mysweldo.payroll.tax;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.DeleteResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll-config/tax")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - Income Tax", description = "Manage income tax bracket configurations")
public class IncomeTaxBracketController {

    private final IncomeTaxBracketService service;
    private final IncomeTaxBracketMapper mapper;

    @PostMapping
    @Operation(summary = "Create income tax bracket", description = "Create a new income tax bracket configuration. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<IncomeTaxBracketDto>> createIncomeTaxBracket(
            @Valid @RequestBody IncomeTaxBracketRequest request) {
        IncomeTaxBracket bracket = service.createIncomeTaxBracket(request);
        return ResponseFactory.created(
                "Income tax bracket created successfully",
                mapper.toDto(bracket)
        );
    }

    @GetMapping
    @Operation(summary = "Get all income tax brackets", description = "Retrieve all income tax brackets with pagination and filters. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<IncomeTaxBracketDto>>> getAllIncomeTaxBrackets(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date") @RequestParam(required = false) LocalDate effectiveDate,
            @Parameter(description = "Filter by minimum income (greater than or equal to)") @RequestParam(required = false) BigDecimal minIncome,
            @Parameter(description = "Filter by maximum income (less than or equal to)") @RequestParam(required = false) BigDecimal maxIncome
    ) {
        Page<IncomeTaxBracket> page = service.getAllIncomeTaxBrackets(
                pageNo, limit, effectiveDate, minIncome, maxIncome);
        List<IncomeTaxBracketDto> brackets = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "Income tax brackets retrieved successfully",
                brackets,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get income tax bracket by ID", description = "Retrieve a specific income tax bracket. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<IncomeTaxBracketDto>> getIncomeTaxBracketById(
            @Parameter(description = "Bracket ID") @PathVariable UUID id) {
        IncomeTaxBracket bracket = service.getIncomeTaxBracketById(id);
        return ResponseFactory.ok(
                "Income tax bracket retrieved successfully",
                mapper.toDto(bracket)
        );
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup income tax bracket by income", description = "Find the income tax bracket for a given income and date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<IncomeTaxBracketDto>> getIncomeTaxBracketByIncome(
            @Parameter(description = "Income amount") @RequestParam BigDecimal income,
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        IncomeTaxBracket bracket = service.getIncomeTaxBracketByIncomeAndDate(income, effectiveDate);
        return ResponseFactory.ok(
                "Income tax bracket found for income",
                mapper.toDto(bracket)
        );
    }

    @GetMapping("/by-date")
    @Operation(summary = "Get all brackets for a date", description = "Retrieve all income tax brackets for a specific effective date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<IncomeTaxBracketDto>>> getIncomeTaxBracketsByDate(
            @Parameter(description = "Effective date (defaults to today)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        LocalDate date = effectiveDate != null ? effectiveDate : LocalDate.now();
        List<IncomeTaxBracket> brackets = service.getAllIncomeTaxBracketsByDate(date);
        List<IncomeTaxBracketDto> dtos = brackets.stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "Income tax brackets retrieved successfully",
                dtos
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update income tax bracket", description = "Update an existing income tax bracket. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<IncomeTaxBracketDto>> updateIncomeTaxBracket(
            @Parameter(description = "Bracket ID") @PathVariable UUID id,
            @Valid @RequestBody IncomeTaxBracketRequest request) {
        IncomeTaxBracket bracket = service.updateIncomeTaxBracket(id, request);
        return ResponseFactory.ok(
                "Income tax bracket updated successfully",
                mapper.toDto(bracket)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete income tax bracket", description = "Soft delete an income tax bracket. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deleteIncomeTaxBracket(
            @Parameter(description = "Bracket ID") @PathVariable UUID id) {
        service.deleteIncomeTaxBracket(id);
        return ResponseFactory.ok(
                "Income tax bracket deleted successfully",
                new DeleteResponse("IncomeTaxBracket", id)
        );
    }
}
