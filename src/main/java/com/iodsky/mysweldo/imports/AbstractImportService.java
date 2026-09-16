package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.common.StorageService;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Base class for async CSV imports. Parses the uploaded file with OpenCSV,
 * persists records row-by-row (each save commits independently) and records
 * skipped rows into import_job_error up to a skip limit.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractImportService<T> {

    protected static final int SKIP_LIMIT = 100;
    private static final int STATUS_SAVE_INTERVAL = 10;

    private final ImportJobRepository importJobRepository;
    private final ImportJobErrorRepository importJobErrorRepository;
    private final StorageService storageService;

    @Async("importTaskExecutor")
    public void runImport(UUID importJobId) {
        ImportJob job = importJobRepository.findById(importJobId)
                .orElseThrow(() -> new IllegalArgumentException("Import job not found: " + importJobId));

        job.setStatus(ImportStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job = importJobRepository.save(job);

        long skipCount = 0;

        try (InputStream in = storageService.get(job.getFileName());
             Reader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(getRecordType())
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            List<T> records = csvToBean.parse();
            job.setReadCount(records.size());
            job = importJobRepository.save(job);

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
                    job = importJobRepository.save(job);
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
            job = importJobRepository.save(job);
            storageService.delete(job.getFileName());
        }
    }

    protected String duplicateReason(String column, String value) {
        return "Duplicate " + column + ": " + value;
    }

    protected abstract Class<T> getRecordType();

    protected abstract void importRecord(T record);

    protected abstract String reasonFor(Throwable t, T record);
}