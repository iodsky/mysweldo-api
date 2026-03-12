package com.iodsky.mysweldo.tax;

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
@RequestMapping("/tax-brackets")
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - Income Tax", description = "Manage income tax bracket configurations")
public class TaxBracketController {

    private final TaxBracketService service;
    private final TaxBracketMapper mapper;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create income tax bracket", description = "Create a new income tax bracket configuration. Requires PAYROLL role.")
    public ApiResponse<TaxBracketDto> createIncomeTaxBracket(
            @Valid @RequestBody TaxBracketRequest request) {
        TaxBracket bracket = service.createIncomeTaxBracket(request);
        return ResponseFactory.success(
                "Income tax bracket created successfully",
                mapper.toDto(bracket)
        );
    }

    @GetMapping
    @Operation(summary = "Get all income tax brackets", description = "Retrieve all income tax brackets with pagination and filters. Requires PAYROLL role.")
    public ApiResponse<List<TaxBracketDto>> getAllIncomeTaxBrackets(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date") @RequestParam(required = false) LocalDate effectiveDate,
            @Parameter(description = "Filter by minimum income (greater than or equal to)") @RequestParam(required = false) BigDecimal minIncome,
            @Parameter(description = "Filter by maximum income (less than or equal to)") @RequestParam(required = false) BigDecimal maxIncome
    ) {
        Page<TaxBracket> page = service.getAllIncomeTaxBrackets(
                pageNo, limit, effectiveDate, minIncome, maxIncome);
        List<TaxBracketDto> brackets = page.getContent().stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "Income tax brackets retrieved successfully",
                brackets,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get income tax bracket by ID", description = "Retrieve a specific income tax bracket. Requires PAYROLL role.")
    public ApiResponse<TaxBracketDto> getIncomeTaxBracketById(
            @Parameter(description = "Bracket ID") @PathVariable UUID id) {
        TaxBracket bracket = service.getIncomeTaxBracketById(id);
        return ResponseFactory.success(
                "Income tax bracket retrieved successfully",
                mapper.toDto(bracket)
        );
    }

    @GetMapping("/lookup")
    @Operation(summary = "Lookup income tax bracket by income", description = "Find the income tax bracket for a given income and date. Requires PAYROLL role.")
    public ApiResponse<TaxBracketDto> getIncomeTaxBracketByIncome(
            @Parameter(description = "Income amount") @RequestParam BigDecimal income,
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        TaxBracket bracket = service.getIncomeTaxBracketByIncomeAndDate(income, effectiveDate);
        return ResponseFactory.success(
                "Income tax bracket found for income",
                mapper.toDto(bracket)
        );
    }

    @GetMapping("/by-date")
    @Operation(summary = "Get all brackets for a date", description = "Retrieve all income tax brackets for a specific effective date. Requires PAYROLL role.")
    public ApiResponse<List<TaxBracketDto>> getIncomeTaxBracketsByDate(
            @Parameter(description = "Effective date (defaults to today)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        LocalDate date = effectiveDate != null ? effectiveDate : LocalDate.now();
        List<TaxBracket> brackets = service.getAllIncomeTaxBracketsByDate(date);
        List<TaxBracketDto> dtos = brackets.stream()
                .map(mapper::toDto)
                .toList();

        return ResponseFactory.success(
                "Income tax brackets retrieved successfully",
                dtos
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update income tax bracket", description = "Update an existing income tax bracket. Requires PAYROLL role.")
    public ApiResponse<TaxBracketDto> updateIncomeTaxBracket(
            @Parameter(description = "Bracket ID") @PathVariable UUID id,
            @Valid @RequestBody TaxBracketRequest request) {
        TaxBracket bracket = service.updateIncomeTaxBracket(id, request);
        return ResponseFactory.success(
                "Income tax bracket updated successfully",
                mapper.toDto(bracket)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete income tax bracket", description = "Soft delete an income tax bracket. Requires PAYROLL role.")
    public ApiResponse<Void> deleteIncomeTaxBracket(
            @Parameter(description = "Bracket ID") @PathVariable UUID id) {
        service.deleteIncomeTaxBracket(id);
        return ResponseFactory.success("Income tax bracket deleted successfully");
    }
}
