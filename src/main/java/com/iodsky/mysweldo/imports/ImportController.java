package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.common.response.PageDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "CSV Imports", description = "CSV import endpoints")
public class ImportController {

    private final EmployeeImportService employeeImportService;
    private final UserImportService userImportService;
    private final ImportJobService importJobService;
    private final ImportJobErrorRepository importJobErrorRepository;

    @Value("${import.upload.directory}")
    private String uploadDirectory;

    @PreAuthorize("hasAnyRole('HR', 'IT', 'SUPERUSER')")
    @PostMapping(value = "/import-employees", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import employees from CSV file",
            description = "Upload a CSV file to import employees asynchronously. Returns an import job ID for tracking.",
            operationId = "importEmployees"
    )
    public ImportJobLaunchResponse importEmployees(@RequestPart("file") MultipartFile file) {
        String fileName = saveCsvFile(file);
        ImportJob job = importJobService.launchImport(ImportType.EMPLOYEE, fileName);
        employeeImportService.runImport(job.getId());

        return ImportJobLaunchResponse.builder()
                .importJobId(job.getId())
                .fileName(fileName)
                .message("Employee import launched successfully")
                .build();
    }

    @PreAuthorize("hasAnyRole('IT', 'SUPERUSER')")
    @PostMapping(value = "/import-users", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Import users from CSV file",
            description = "Upload a CSV file to import users asynchronously. Returns an import job ID for tracking. Restricted to IT role only.",
            operationId = "importUsers"
    )
    public ImportJobLaunchResponse importUsers(@RequestPart("file") MultipartFile file) {
        String fileName = saveCsvFile(file);
        ImportJob job = importJobService.launchImport(ImportType.USER, fileName);
        userImportService.runImport(job.getId());

        return ImportJobLaunchResponse.builder()
                .importJobId(job.getId())
                .fileName(fileName)
                .message("User import launched successfully")
                .build();
    }

    @PreAuthorize("hasAnyRole('HR', 'IT', 'PAYROLL', 'SUPERUSER')")
    @GetMapping
    @Operation(
            summary = "List import jobs",
            description = "Retrieve a paginated list of import jobs, newest first, optionally filtered by type and/or status. Requires HR, IT, PAYROLL, or SUPERUSER role.",
            operationId = "getAllImportJobs"
    )
    public PageDto<ImportJobSummaryDto> getAllImportJobs(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by import type") @RequestParam(required = false) ImportType type,
            @Parameter(description = "Filter by import status") @RequestParam(required = false) ImportStatus status
    ) {
        Page<ImportJobSummaryDto> page = importJobService.getAllImportJobs(pageNo, limit, type, status);

        return PageDto.of(page);
    }

    @PreAuthorize("hasAnyRole('HR', 'IT', 'PAYROLL', 'SUPERUSER')")
    @GetMapping("/{importJobId}")
    @Operation(
            summary = "Get import job details",
            description = "Retrieve detailed information about an import job including status, counts and per-row failures.",
            operationId = "getImportJobDetails"
    )
    public ImportJobDetailsResponse getImportJobDetails(@PathVariable UUID importJobId) {
        ImportJob job = importJobService.getImportJob(importJobId);

        List<ImportJobErrorDto> failures = importJobErrorRepository
                .findAllByImportJob_IdOrderByRowNumberAsc(importJobId)
                .stream()
                .map(error -> ImportJobErrorDto.builder()
                        .rowNumber(error.getRowNumber())
                        .reason(error.getReason())
                        .build())
                .toList();

        return ImportJobDetailsResponse.builder()
                .importJobId(job.getId())
                .type(job.getType())
                .status(job.getStatus())
                .fileName(job.getFileName())
                .readCount(job.getReadCount())
                .writeCount(job.getWriteCount())
                .skipCount(job.getSkipCount())
                .startedAt(job.getStartedAt())
                .finishedAt(job.getFinishedAt())
                .errorMessage(job.getErrorMessage())
                .failures(failures)
                .build();
    }

    /**
     * Upload a CSV file to the upload directory with a timestamped filename.
     *
     * @param file the multipart file to upload
     * @return the generated filename
     */
    private String saveCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".csv")) {
            throw new IllegalArgumentException("Only CSV files are supported");
        }

        File uploadDir = new File(uploadDirectory);
        if (!uploadDir.exists() && !uploadDir.mkdirs()) {
            throw new IllegalStateException("Failed to create upload directory: " + uploadDirectory);
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = timestamp + "_" + file.getOriginalFilename();

        try {
            Path filePath = Paths.get(uploadDirectory, fileName);
            Files.copy(file.getInputStream(), filePath);
        } catch (Exception e) {
            log.error("Failed to save uploaded file", e);
            throw new IllegalStateException("Failed to save uploaded file: " + e.getMessage(), e);
        }

        log.info("File uploaded successfully: {}", fileName);
        return fileName;
    }

}