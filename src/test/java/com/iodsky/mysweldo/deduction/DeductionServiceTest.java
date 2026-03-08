package com.iodsky.mysweldo.deduction;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeductionServiceTest {

    @InjectMocks
    private DeductionService service;

    @Mock
    private DeductionRepository repository;

    @Nested
    class CreateDeductionTests {

        @Test
        void shouldCreateAndReturnDeductionWhenCodeDoesNotExist() {
            DeductionRequest request = DeductionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            Deduction saved = Deduction.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(false);
            when(repository.save(any(Deduction.class))).thenReturn(saved);

            Deduction result = service.createDeduction(request);

            assertEquals("SSS", result.getCode());
            assertEquals("Social Security System", result.getDescription());
        }

        @Test
        void shouldThrowConflictWhenDeductionWithSameCodeAlreadyExists() {
            DeductionRequest request = DeductionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(true);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.createDeduction(request)
            );

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        }
    }

    @Nested
    class GetAllDeductionsTests {

        @Test
        void shouldReturnPaginatedDeductionsForValidPageAndLimit() {
            List<Deduction> deductions = List.of(
                    Deduction.builder().code("SSS").description("Social Security System").build(),
                    Deduction.builder().code("PHIC").description("PhilHealth").build()
            );
            Page<Deduction> page = new PageImpl<>(deductions, PageRequest.of(0, 10), 2);

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

            Page<Deduction> result = service.getAllDeductions(0, 10);

            assertEquals(2, result.getTotalElements());
            assertEquals(2, result.getContent().size());
        }

        @Test
        void shouldReturnEmptyPageWhenNoDeductionsExist() {
            Page<Deduction> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(repository.findAll(PageRequest.of(0, 10))).thenReturn(emptyPage);

            Page<Deduction> result = service.getAllDeductions(0, 10);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetDeductionByCodeTests {

        @Test
        void shouldReturnDeductionWhenItExists() {
            Deduction deduction = Deduction.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findByCode("SSS")).thenReturn(Optional.of(deduction));

            Deduction result = service.getDeductionByCode("SSS");

            assertEquals("SSS", result.getCode());
            assertEquals("Social Security System", result.getDescription());
        }

        @Test
        void shouldThrowNotFoundWhenDeductionDoesNotExist() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getDeductionByCode("UNKNOWN")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenDeductionIsSoftDeleted() {
            // @SQLRestriction filters out soft-deleted rows at the database level,
            // so findByCode returns empty for soft-deleted records
            when(repository.findByCode("SSS")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getDeductionByCode("SSS")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class UpdateDeductionTests {

        @Test
        void shouldUpdateAndReturnDeductionWithNewDescriptionWhenItExists() {
            Deduction existing = Deduction.builder()
                    .code("SSS")
                    .description("Old Description")
                    .build();

            DeductionRequest request = DeductionRequest.builder()
                    .code("SSS")
                    .description("Updated Description")
                    .build();

            when(repository.findByCode("SSS")).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            Deduction result = service.updateDeduction("SSS", request);

            assertEquals("Updated Description", result.getDescription());
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentDeduction() {
            DeductionRequest request = DeductionRequest.builder()
                    .code("UNKNOWN")
                    .description("Some Description")
                    .build();

            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updateDeduction("UNKNOWN", request)
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }

    @Nested
    class DeleteDeductionTests {

        @Test
        void shouldSoftDeleteDeductionBySettingDeletedAtWhenItExists() {
            Deduction deduction = Deduction.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findByCode("SSS")).thenReturn(Optional.of(deduction));

            service.deleteDeduction("SSS");

            assertNotNull(deduction.getDeletedAt());
            verify(repository).save(deduction);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentDeduction() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> service.deleteDeduction("UNKNOWN")
            );

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        }
    }
}