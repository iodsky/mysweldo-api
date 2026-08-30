package com.iodsky.mysweldo.benefit;

import com.iodsky.mysweldo.payroll.item.PayrollBenefitRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BenefitServiceTest {

    @Mock
    private BenefitRepository repository;

    @Mock
    private PayrollBenefitRepository payrollBenefitRepository;

    @InjectMocks
    private BenefitService service;

    @Nested
    class CreateBenefitTests {

        @Test
        void shouldThrowConflictWhenBenefitAlreadyExists() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(true);

            assertThatThrownBy(() -> service.createBenefit(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequestWhenTaxableBenefitHasNonTaxableLimit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .nonTaxableLimit(new BigDecimal(1))
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);

            assertThatThrownBy(() -> service.createBenefit(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldCreateNonTaxableBenefitWithNonTaxableLimit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(false)
                    .nonTaxableLimit(new BigDecimal(1500))
                    .build();
            Benefit benefit = Benefit.builder()
                    .code("TRANSPO")
                    .taxable(false)
                    .nonTaxableLimit(new BigDecimal(1500))
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);
            when(repository.save(any())).thenReturn(benefit);

            Benefit result = service.createBenefit(request);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("TRANSPO");

            ArgumentCaptor<Benefit> captor = ArgumentCaptor.forClass(Benefit.class);
            verify(repository).save(captor.capture());
            Benefit saved = captor.getValue();
            assertThat(saved.getCode()).isEqualTo("TRANSPO");
            assertThat(saved.isTaxable()).isFalse();
            assertThat(saved.getNonTaxableLimit()).isEqualByComparingTo(new BigDecimal(1500));
        }

        @Test
        void shouldCreateTaxableBenefitWithoutNonTaxableLimit() {
            BenefitRequest request = BenefitRequest.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .build();
            Benefit benefit = Benefit.builder()
                    .code("TRANSPO")
                    .taxable(true)
                    .build();
            when(repository.existsById("TRANSPO")).thenReturn(false);
            when(repository.save(any())).thenReturn(benefit);

            Benefit result = service.createBenefit(request);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo("TRANSPO");

            ArgumentCaptor<Benefit> captor = ArgumentCaptor.forClass(Benefit.class);
            verify(repository).save(captor.capture());
            Benefit saved = captor.getValue();
            assertThat(saved.getCode()).isEqualTo("TRANSPO");
            assertThat(saved.isTaxable()).isTrue();
            assertThat(saved.getNonTaxableLimit()).isNull();
        }
    }

    @Nested
    class GetBenefitByCodeTests {

        @Test
        void shouldReturnBenefitWhenItExistsAndIsNotDeleted() {
            Benefit benefit = Benefit.builder().code("TRANSPO").description("Transport allowance").build();
            when(repository.findById("TRANSPO")).thenReturn(Optional.of(benefit));

            Benefit result = service.getBenefitByCode("TRANSPO");

            assertThat(result).isEqualTo(benefit);
        }

        @Test
        void shouldThrow404WhenBenefitDoesNotExist() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getBenefitByCode("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrow404WhenBenefitIsSoftDeleted() {
            Benefit deleted = Benefit.builder().code("TRANSPO").build();
            deleted.setDeletedAt(Instant.now());
            when(repository.findById("TRANSPO")).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> service.getBenefitByCode("TRANSPO"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class UpdateBenefitTests {

        @Test
        void shouldUpdateDescriptionWhenBenefitExists() {
            Benefit existing = Benefit.builder().code("TRANSPO").description("Old description").build();
            BenefitRequest request = BenefitRequest.builder().code("TRANSPO").description("New description").build();

            when(repository.findById("TRANSPO")).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            Benefit result = service.updateBenefit("TRANSPO", request);

            assertThat(result.getDescription()).isEqualTo("New description");
            verify(repository).save(existing);
        }

        @Test
        void shouldThrow404WhenUpdatingNonExistentBenefit() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateBenefit("UNKNOWN", BenefitRequest.builder().build()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class DeleteBenefitTests {

        @Test
        void shouldSoftDeleteBenefitBySettingDeletedAt() {
            Benefit existing = Benefit.builder().code("TRANSPO").build();
            when(repository.findById("TRANSPO")).thenReturn(Optional.of(existing));

            service.deleteBenefit("TRANSPO");

            assertThat(existing.getDeletedAt()).isNotNull();
            verify(repository).save(existing);
        }

        @Test
        void shouldThrow404WhenDeletingNonExistentBenefit() {
            when(repository.findById("UNKNOWN")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteBenefit("UNKNOWN"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowConflictWhenBenefitIsReferencedByPayrollItems() {
            Benefit existing = Benefit.builder().code("TRANSPO").build();
            when(repository.findById("TRANSPO")).thenReturn(Optional.of(existing));
            when(payrollBenefitRepository.existsByBenefit_Code("TRANSPO")).thenReturn(true);

            assertThatThrownBy(() -> service.deleteBenefit("TRANSPO"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any());
        }
    }
}
