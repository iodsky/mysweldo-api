package com.iodsky.mysweldo.imports;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImportJobService {

    private final ImportJobRepository importJobRepository;

    public ImportJob launchImport(ImportType type, String fileName) {
        ImportJob job = ImportJob.builder()
                .type(type)
                .fileName(fileName)
                .status(ImportStatus.PENDING)
                .readCount(0)
                .writeCount(0)
                .skipCount(0)
                .build();

        return importJobRepository.save(job);
    }

    public ImportJob getImportJob(UUID importJobId) {
        return importJobRepository.findById(importJobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Import job " + importJobId + " not found"));
    }

    public Page<ImportJobSummaryDto> getAllImportJobs(int page, int limit, ImportType type, ImportStatus status) {
        Pageable pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        Page<ImportJob> result;

        if (type != null && status != null) {
            result = importJobRepository.findAllByTypeAndStatus(type, status, pageable);
        } else if (type != null) {
            result = importJobRepository.findAllByType(type, pageable);
        } else if (status != null) {
            result = importJobRepository.findAllByStatus(status, pageable);
        } else {
            result = importJobRepository.findAll(pageable);
        }

        return result.map(this::toSummary);
    }

    private ImportJobSummaryDto toSummary(ImportJob job) {
        return ImportJobSummaryDto.builder()
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
                .createdAt(job.getCreatedAt())
                .build();
    }

}