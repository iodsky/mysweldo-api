package com.iodsky.mysweldo.imports;

import lombok.RequiredArgsConstructor;
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

}