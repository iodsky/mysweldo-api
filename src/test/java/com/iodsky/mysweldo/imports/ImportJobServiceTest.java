package com.iodsky.mysweldo.imports;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobServiceTest {

    @InjectMocks
    private ImportJobService service;

    @Mock
    private ImportJobRepository importJobRepository;

    private ImportJob job(ImportType type, ImportStatus status) {
        return ImportJob.builder()
                .id(UUID.randomUUID())
                .type(type)
                .status(status)
                .fileName("file.csv")
                .readCount(10)
                .writeCount(8)
                .skipCount(2)
                .startedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .finishedAt(Instant.parse("2026-01-01T00:01:00Z"))
                .build();
    }

    @Test
    void getAllImportJobs_noFiltersUsesFindAll() {
        ImportJob repoJob = job(ImportType.EMPLOYEE, ImportStatus.COMPLETED);
        when(importJobRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(repoJob)));

        Page<ImportJobSummaryDto> result = service.getAllImportJobs(0, 10, null, null);

        assertThat(result.getContent()).hasSize(1);
        ImportJobSummaryDto dto = result.getContent().get(0);
        assertThat(dto.getImportJobId()).isEqualTo(repoJob.getId());
        assertThat(dto.getType()).isEqualTo(ImportType.EMPLOYEE);
        assertThat(dto.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(dto.getFileName()).isEqualTo("file.csv");
        assertThat(dto.getReadCount()).isEqualTo(10);
        assertThat(dto.getWriteCount()).isEqualTo(8);
        assertThat(dto.getSkipCount()).isEqualTo(2);
        assertThat(dto.getCreatedAt()).isEqualTo(repoJob.getCreatedAt());

        verify(importJobRepository).findAll(any(Pageable.class));
        verify(importJobRepository, never()).findAllByType(any(), any(Pageable.class));
        verify(importJobRepository, never()).findAllByStatus(any(), any(Pageable.class));
        verify(importJobRepository, never()).findAllByTypeAndStatus(any(), any(), any(Pageable.class));
    }

    @Test
    void getAllImportJobs_filtersByType() {
        when(importJobRepository.findAllByType(eq(ImportType.USER), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job(ImportType.USER, ImportStatus.PENDING))));

        Page<ImportJobSummaryDto> result = service.getAllImportJobs(0, 10, ImportType.USER, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(ImportType.USER);

        verify(importJobRepository).findAllByType(eq(ImportType.USER), any(Pageable.class));
        verify(importJobRepository, never()).findAll(any(Pageable.class));
        verify(importJobRepository, never()).findAllByStatus(any(), any(Pageable.class));
        verify(importJobRepository, never()).findAllByTypeAndStatus(any(), any(), any(Pageable.class));
    }

    @Test
    void getAllImportJobs_filtersByStatus() {
        when(importJobRepository.findAllByStatus(eq(ImportStatus.FAILED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job(ImportType.EMPLOYEE, ImportStatus.FAILED))));

        Page<ImportJobSummaryDto> result = service.getAllImportJobs(0, 10, null, ImportStatus.FAILED);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ImportStatus.FAILED);

        verify(importJobRepository).findAllByStatus(eq(ImportStatus.FAILED), any(Pageable.class));
        verify(importJobRepository, never()).findAll(any(Pageable.class));
        verify(importJobRepository, never()).findAllByType(any(), any(Pageable.class));
        verify(importJobRepository, never()).findAllByTypeAndStatus(any(), any(), any(Pageable.class));
    }

    @Test
    void getAllImportJobs_filtersByTypeAndStatus() {
        when(importJobRepository.findAllByTypeAndStatus(eq(ImportType.EMPLOYEE), eq(ImportStatus.RUNNING), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(job(ImportType.EMPLOYEE, ImportStatus.RUNNING))));

        Page<ImportJobSummaryDto> result = service.getAllImportJobs(0, 10, ImportType.EMPLOYEE, ImportStatus.RUNNING);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getType()).isEqualTo(ImportType.EMPLOYEE);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ImportStatus.RUNNING);

        verify(importJobRepository).findAllByTypeAndStatus(eq(ImportType.EMPLOYEE), eq(ImportStatus.RUNNING), any(Pageable.class));
        verify(importJobRepository, never()).findAll(any(Pageable.class));
        verify(importJobRepository, never()).findAllByType(any(), any(Pageable.class));
        verify(importJobRepository, never()).findAllByStatus(any(), any(Pageable.class));
    }

    @Test
    void getAllImportJobs_sortsByCreatedAtDescending() {
        when(importJobRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllImportJobs(0, 10, null, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(importJobRepository).findAll(captor.capture());

        Pageable captured = captor.getValue();
        assertThat(captured.getSort().isSorted()).isTrue();
        assertThat(captured.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captured.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }
}