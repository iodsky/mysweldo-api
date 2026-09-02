package com.iodsky.mysweldo.imports;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base class for async CSV imports. Parses the uploaded file with OpenCSV,
 * persists records row-by-row (each save commits independently) and records
 * skipped rows into import_job_error up to a skip limit.
 */
@Slf4j
public abstract class AbstractImportService<T> {

    protected static final int SKIP_LIMIT = 100;
    private static final int STATUS_SAVE_INTERVAL = 10;

    private final ImportJobRepository importJobRepository;
    private final ImportJobErrorRepository importJobErrorRepository;

    @Value("${import.upload.directory}")
    private String uploadDirectory;

    protected AbstractImportService(ImportJobRepository importJobRepository,
                                    ImportJobErrorRepository importJobErrorRepository) {
        this.importJobRepository = importJobRepository;
        this.importJobErrorRepository = importJobErrorRepository;
    }

    @Async("importTaskExecutor")
    public void runImport(UUID importJobId) {
        ImportJob job = importJobRepository.findById(importJobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + importJobId));

        job.setStatus(ImportStatus.RUNNING);
        job.setStartedAt(Instant.now());
        importJobRepository.save(job);

        Path filePath = Paths.get(uploadDirectory, job.getFileName());
        long skipCount = 0;

        try (Reader reader = Files.newBufferedReader(filePath)) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(getRecordType())
                    .withSkipLines(1)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<T> records = csvToBean.parse();
            job.setReadCount(records.size());
            importJobRepository.save(job);

            for (int i = 0; i < records.size(); i++) {
                T record = records.get(i);
                try {
                    importRecord(record);
                    job.setWriteCount(job.getWriteCount() + 1);
                } catch (RuntimeException e) {
                    if (++skipCount > SKIP_LIMIT) {
                        throw new IllegalStateException(
                                "Skip limit of " + SKIP_LIMIT + " exceeded, aborting import. " + e.getMessage(), e);
                    }
                    importJobErrorRepository.save(ImportJobError.builder()
                            .importJob(job)
                            .rowNumber(i + 2L)
                            .reason(reasonFor(e, record))
                            .build());
                    job.setSkipCount(skipCount);
                }

                if ((i + 1) % STATUS_SAVE_INTERVAL == 0) {
                    importJobRepository.save(job);
                }
            }

            job.setSkipCount(skipCount);
            job.setStatus(ImportStatus.COMPLETED);
            job.setErrorMessage(null);
        } catch (Exception e) {
            log.error("Import job {} failed", importJobId, e);
            job.setStatus(ImportStatus.FAILED);
            job.setErrorMessage(e.getMessage());
        } finally {
            job.setFinishedAt(Instant.now());
            importJobRepository.save(job);
            deleteFile(filePath);
        }
    }

    protected String duplicateReason(String column, String value) {
        return "Duplicate " + column + ": " + value;
    }

    private void deleteFile(Path filePath) {
        try {
            Files.deleteIfExists(filePath);
            log.info("Deleted uploaded file: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete uploaded file: {}. Error: {}", filePath, e.getMessage(), e);
        }
    }

    protected abstract Class<T> getRecordType();

    protected abstract void importRecord(T record);

    protected abstract String reasonFor(Throwable t, T record);
}