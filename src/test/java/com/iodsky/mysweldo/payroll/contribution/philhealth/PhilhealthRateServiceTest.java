package com.iodsky.mysweldo.payroll.contribution.philhealth;

import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRateRepository;
import com.iodsky.mysweldo.philhealth.PhilhealthRateRequest;
import com.iodsky.mysweldo.philhealth.PhilhealthRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhilhealthRateServiceTest {

    @InjectMocks
    private PhilhealthRateService service;

    @Mock
    private PhilhealthRateRepository repository;

    private PhilhealthRate rateTable;
    private PhilhealthRateRequest request;

    @BeforeEach
    void setUp() {
        rateTable = PhilhealthRate.builder()
                .id(UUID.randomUUID())
                .premiumRate(new BigDecimal("0.0500"))
                .maxSalaryCap(new BigDecimal("100000.00"))
                .minSalaryFloor(new BigDecimal("10000.00"))
                .fixedContribution(new BigDecimal("500.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        request = PhilhealthRateRequest.builder()
                .premiumRate(new BigDecimal("0.0500"))
                .maxSalaryCap(new BigDecimal("100000.00"))
                .minSalaryFloor(new BigDecimal("10000.00"))
                .fixedContribution(new BigDecimal("500.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Nested
    class CreatePhilhealthRateTests {

        @Test
        void shouldCreateRateTableWhenNoExistingRecordForEffectiveDate() {
            when(repository.findLatestByEffectiveDate(request.getEffectiveDate()))
                    .thenReturn(Optional.empty());
            when(repository.save(any(PhilhealthRate.class))).thenReturn(rateTable);

            PhilhealthRate result = service.createPhilhealthRate(request);

            assertThat(result).isNotNull();
            assertThat(result.getPremiumRate()).isEqualTo(request.getPremiumRate());
            assertThat(result.getMaxSalaryCap()).isEqualTo(request.getMaxSalaryCap());
            assertThat(result.getMinSalaryFloor()).isEqualTo(request.getMinSalaryFloor());
            assertThat(result.getFixedContribution()).isEqualTo(request.getFixedContribution());
            assertThat(result.getEffectiveDate()).isEqualTo(request.getEffectiveDate());
        }

        @Test
        void shouldThrowConflictWhenRateTableAlreadyExistsForEffectiveDate() {
            when(repository.findLatestByEffectiveDate(request.getEffectiveDate()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.createPhilhealthRate(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(rse.getReason()).contains(request.getEffectiveDate().toString());
                    });

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class GetAllPhilhealthRateTablesTests {

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedRateTablesWhenNoEffectiveDateFilterProvided() {
            Page<PhilhealthRate> expectedPage = new PageImpl<>(List.of(rateTable));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<PhilhealthRate> result = service.getAllPhilhealthRates(0, 10, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(rateTable);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedRateTablesFilteredByEffectiveDateWhenDateProvided() {
            Page<PhilhealthRate> expectedPage = new PageImpl<>(List.of(rateTable));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<PhilhealthRate> result = service.getAllPhilhealthRates(0, 10, LocalDate.of(2024, 6, 1));

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnEmptyPageWhenNoRateTablesExist() {
            Page<PhilhealthRate> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<PhilhealthRate> result = service.getAllPhilhealthRates(0, 10, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetPhilhealthRateByIdTests {

        @Test
        void shouldReturnRateTableWhenItExistsAndIsNotDeleted() {
            when(repository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            PhilhealthRate result = service.getPhilhealthRateById(rateTable.getId());

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenRateTableDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPhilhealthRateById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }

        @Test
        void shouldThrowNotFoundWhenRateTableIsSoftDeleted() {
            rateTable.setDeletedAt(Instant.now());
            when(repository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.getPhilhealthRateById(rateTable.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetLatestPhilhealthRateTests {

        @Test
        void shouldReturnLatestRateTableForGivenDate() {
            LocalDate date = LocalDate.of(2024, 6, 1);
            when(repository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.of(rateTable));

            PhilhealthRate result = service.getLatestPhilhealthRate(date);

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenNoRateTableExistsForGivenDate() {
            LocalDate date = LocalDate.of(2020, 1, 1);
            when(repository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLatestPhilhealthRate(date))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(date.toString());
                    });
        }
    }

    @Nested
    class UpdatePhilhealthRateTests {

        @Test
        void shouldUpdateFieldsAndReturnUpdatedRateTableWhenExists() {
            PhilhealthRateRequest updateRequest = PhilhealthRateRequest.builder()
                    .premiumRate(new BigDecimal("0.0600"))
                    .maxSalaryCap(new BigDecimal("120000.00"))
                    .minSalaryFloor(new BigDecimal("10000.00"))
                    .fixedContribution(new BigDecimal("500.00"))
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(repository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(repository.save(any(PhilhealthRate.class))).thenAnswer(inv -> inv.getArgument(0));

            PhilhealthRate result = service.updatePhilhealthRate(rateTable.getId(), updateRequest);

            assertThat(result.getPremiumRate()).isEqualTo(updateRequest.getPremiumRate());
            assertThat(result.getMaxSalaryCap()).isEqualTo(updateRequest.getMaxSalaryCap());
            assertThat(result.getEffectiveDate()).isEqualTo(updateRequest.getEffectiveDate());
        }

        @Test
        void shouldNotModifyMinSalaryFloorOrFixedContributionDuringUpdate() {
            BigDecimal originalMinSalaryFloor = rateTable.getMinSalaryFloor();
            BigDecimal originalFixedContribution = rateTable.getFixedContribution();

            PhilhealthRateRequest updateRequest = PhilhealthRateRequest.builder()
                    .premiumRate(new BigDecimal("0.0600"))
                    .maxSalaryCap(new BigDecimal("120000.00"))
                    .minSalaryFloor(new BigDecimal("99999.00"))
                    .fixedContribution(new BigDecimal("99999.00"))
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(repository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(repository.save(any(PhilhealthRate.class))).thenAnswer(inv -> inv.getArgument(0));

            PhilhealthRate result = service.updatePhilhealthRate(rateTable.getId(), updateRequest);

            assertThat(result.getMinSalaryFloor()).isEqualTo(originalMinSalaryFloor);
            assertThat(result.getFixedContribution()).isEqualTo(originalFixedContribution);
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentRateTable() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePhilhealthRate(unknownId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingSoftDeletedRateTable() {
            rateTable.setDeletedAt(Instant.now());
            when(repository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.updatePhilhealthRate(rateTable.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}

