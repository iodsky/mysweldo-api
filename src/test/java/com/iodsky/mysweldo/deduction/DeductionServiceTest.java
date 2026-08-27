package com.iodsky.mysweldo.deduction;

import com.iodsky.mysweldo.payroll.core.PayrollDeductionRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeductionServiceTest {

    @InjectMocks
    private DeductionService service;

    @Mock
    private DeductionRepository repository;

    @Mock
    private PayrollDeductionRepository payrollDeductionRepository;

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

            assertThat(result.getCode()).isEqualTo("SSS");
            assertThat(result.getDescription()).isEqualTo("Social Security System");
        }

        @Test
        void shouldThrowConflictWhenDeductionWithSameCodeAlreadyExists() {
            DeductionRequest request = DeductionRequest.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.existsById("SSS")).thenReturn(true);

            assertThatThrownBy(() -> service.createDeduction(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
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

            assertThat(result.getCode()).isEqualTo("SSS");
            assertThat(result.getDescription()).isEqualTo("Social Security System");
        }

        @Test
        void shouldThrowNotFoundWhenDeductionDoesNotExist() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDeductionByCode("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowNotFoundWhenDeductionIsSoftDeleted() {
            // @SQLRestriction filters out soft-deleted rows at the database level
            when(repository.findByCode("SSS")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getDeductionByCode("SSS"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
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

            assertThat(result.getDescription()).isEqualTo("Updated Description");
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentDeduction() {
            DeductionRequest request = DeductionRequest.builder()
                    .code("UNKNOWN")
                    .description("Some Description")
                    .build();

            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateDeduction("UNKNOWN", request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
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

            assertThat(deduction.getDeletedAt()).isNotNull();
            verify(repository).save(deduction);
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentDeduction() {
            when(repository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteDeduction("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowConflictWhenDeductionIsReferencedByPayrollItems() {
            Deduction deduction = Deduction.builder()
                    .code("SSS")
                    .description("Social Security System")
                    .build();

            when(repository.findByCode("SSS")).thenReturn(Optional.of(deduction));
            when(payrollDeductionRepository.existsByDeduction_Code("SSS")).thenReturn(true);

            assertThatThrownBy(() -> service.deleteDeduction("SSS"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any());
        }
    }
}
