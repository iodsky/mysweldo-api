package com.iodsky.mysweldo.payroll.tax;

import com.iodsky.mysweldo.tax.TaxBracket;
import com.iodsky.mysweldo.tax.TaxBracketRepository;
import com.iodsky.mysweldo.tax.TaxBracketRequest;
import com.iodsky.mysweldo.tax.TaxBracketService;
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
class TaxBracketServiceTest {

    @InjectMocks
    private TaxBracketService service;

    @Mock
    private TaxBracketRepository repository;

    private TaxBracket bracket;
    private TaxBracketRequest request;

    @BeforeEach
    void setUp() {
        bracket = TaxBracket.builder()
                .id(UUID.randomUUID())
                .minIncome(new BigDecimal("20833.00"))
                .maxIncome(new BigDecimal("33332.00"))
                .baseTax(new BigDecimal("0.00"))
                .marginalRate(new BigDecimal("0.2000"))
                .threshold(new BigDecimal("20833.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        request = TaxBracketRequest.builder()
                .minIncome(new BigDecimal("20833.00"))
                .maxIncome(new BigDecimal("33332.00"))
                .baseTax(new BigDecimal("0.00"))
                .marginalRate(new BigDecimal("0.2000"))
                .threshold(new BigDecimal("20833.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Nested
    class CreateTaxBracketTests {

        @Test
        void shouldReturnSavedBracketWhenAllFieldsAreProvided() {
            when(repository.save(any(TaxBracket.class))).thenReturn(bracket);

            TaxBracket result = service.createIncomeTaxBracket(request);

            assertThat(result).isNotNull();
            assertThat(result.getMinIncome()).isEqualTo(request.getMinIncome());
            assertThat(result.getMaxIncome()).isEqualTo(request.getMaxIncome());
            assertThat(result.getBaseTax()).isEqualTo(request.getBaseTax());
            assertThat(result.getMarginalRate()).isEqualTo(request.getMarginalRate());
            assertThat(result.getThreshold()).isEqualTo(request.getThreshold());
            assertThat(result.getEffectiveDate()).isEqualTo(request.getEffectiveDate());
        }

        @Test
        void shouldPersistBracketWithNullMaxIncomeForOpenEndedTopBracket() {
            TaxBracketRequest openEndedRequest = TaxBracketRequest.builder()
                    .minIncome(new BigDecimal("666667.00"))
                    .maxIncome(null)
                    .baseTax(new BigDecimal("130833.33"))
                    .marginalRate(new BigDecimal("0.3500"))
                    .threshold(new BigDecimal("666667.00"))
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .build();

            TaxBracket openEndedBracket = TaxBracket.builder()
                    .id(UUID.randomUUID())
                    .minIncome(openEndedRequest.getMinIncome())
                    .maxIncome(null)
                    .baseTax(openEndedRequest.getBaseTax())
                    .marginalRate(openEndedRequest.getMarginalRate())
                    .threshold(openEndedRequest.getThreshold())
                    .effectiveDate(openEndedRequest.getEffectiveDate())
                    .build();

            when(repository.save(any(TaxBracket.class))).thenReturn(openEndedBracket);

            TaxBracket result = service.createIncomeTaxBracket(openEndedRequest);

            assertThat(result.getMaxIncome()).isNull();
        }

        @Test
        void shouldPersistBracketWithZeroBaseTaxForFirstBracket() {
            TaxBracketRequest firstBracketRequest = TaxBracketRequest.builder()
                    .minIncome(BigDecimal.ZERO)
                    .maxIncome(new BigDecimal("20832.00"))
                    .baseTax(BigDecimal.ZERO)
                    .marginalRate(BigDecimal.ZERO)
                    .threshold(BigDecimal.ZERO)
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .build();

            TaxBracket zeroBaseTaxBracket = TaxBracket.builder()
                    .id(UUID.randomUUID())
                    .minIncome(BigDecimal.ZERO)
                    .baseTax(BigDecimal.ZERO)
                    .effectiveDate(firstBracketRequest.getEffectiveDate())
                    .build();

            when(repository.save(any(TaxBracket.class))).thenReturn(zeroBaseTaxBracket);

            TaxBracket result = service.createIncomeTaxBracket(firstBracketRequest);

            assertThat(result.getBaseTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldPropagateExceptionWhenRepositorySaveFails() {
            when(repository.save(any(TaxBracket.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> service.createIncomeTaxBracket(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    class GetAllTaxBracketsTests {

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedBracketsWhenNoFiltersApplied() {
            Page<TaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(0, 10, null, null, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(bracket);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByEffectiveDateWhenDateProvided() {
            Page<TaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, LocalDate.of(2024, 1, 1), null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByMinIncomeWhenMinIncomeProvided() {
            Page<TaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, new BigDecimal("20833.00"), null);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByMaxIncomeWhenMaxIncomeIsGreaterThanZero() {
            Page<TaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, null, new BigDecimal("33332.00"));

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldIgnoreMaxIncomeFilterWhenMaxIncomeIsZero() {
            Page<TaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, null, BigDecimal.ZERO);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnEmptyPageWhenNoBracketsMatchFilters() {
            Page<TaxBracket> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<TaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, LocalDate.of(2099, 1, 1), null, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetAllTaxBracketsByDateTests {

        @Test
        void shouldReturnBracketsMatchingGivenEffectiveDate() {
            LocalDate date = LocalDate.of(2024, 1, 1);
            when(repository.findAllByEffectiveDate(date)).thenReturn(List.of(bracket));

            List<TaxBracket> result = service.getAllIncomeTaxBracketsByDate(date);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEffectiveDate()).isEqualTo(date);
        }

        @Test
        void shouldReturnEmptyListWhenNoBracketsExistForGivenDate() {
            LocalDate date = LocalDate.of(2099, 1, 1);
            when(repository.findAllByEffectiveDate(date)).thenReturn(List.of());

            List<TaxBracket> result = service.getAllIncomeTaxBracketsByDate(date);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetTaxBracketByIdTests {

        @Test
        void shouldReturnBracketWhenItExistsAndIsNotDeleted() {
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));

            TaxBracket result = service.getIncomeTaxBracketById(bracket.getId());

            assertThat(result).isEqualTo(bracket);
        }

        @Test
        void shouldThrowNotFoundWhenBracketDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getIncomeTaxBracketById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(unknownId.toString());
                    });
        }

        @Test
        void shouldThrowNotFoundWhenBracketIsSoftDeleted() {
            bracket.setDeletedAt(Instant.now());
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));

            assertThatThrownBy(() -> service.getIncomeTaxBracketById(bracket.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    });
        }
    }

    @Nested
    class GetIncomeTaxBracketByAndDateTests {

        @Test
        void shouldReturnMatchingBracketForGivenIncomeAndDate() {
            BigDecimal income = new BigDecimal("25000.00");
            LocalDate date = LocalDate.of(2024, 1, 1);
            when(repository.findByIncomeAndEffectiveDate(income, date)).thenReturn(bracket);

            TaxBracket result = service.getIncomeTaxBracketByIncomeAndDate(income, date);

            assertThat(result).isEqualTo(bracket);
        }

        @Test
        void shouldThrowNotFoundWhenNoBracketMatchesIncomeAndDate() {
            BigDecimal income = new BigDecimal("99999999.00");
            LocalDate date = LocalDate.of(2099, 1, 1);
            when(repository.findByIncomeAndEffectiveDate(income, date)).thenReturn(null);

            assertThatThrownBy(() -> service.getIncomeTaxBracketByIncomeAndDate(income, date))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(income.toString());
                        assertThat(rse.getReason()).contains(date.toString());
                    });
        }
    }

    @Nested
    class UpdateTaxBracketTests {

        @Test
        void shouldUpdateAllFieldsAndReturnUpdatedBracket() {
            TaxBracketRequest updateRequest = TaxBracketRequest.builder()
                    .minIncome(new BigDecimal("33333.00"))
                    .maxIncome(new BigDecimal("66666.00"))
                    .baseTax(new BigDecimal("2500.00"))
                    .marginalRate(new BigDecimal("0.2500"))
                    .threshold(new BigDecimal("33333.00"))
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            TaxBracket updatedBracket = TaxBracket.builder()
                    .id(bracket.getId())
                    .minIncome(updateRequest.getMinIncome())
                    .maxIncome(updateRequest.getMaxIncome())
                    .baseTax(updateRequest.getBaseTax())
                    .marginalRate(updateRequest.getMarginalRate())
                    .threshold(updateRequest.getThreshold())
                    .effectiveDate(updateRequest.getEffectiveDate())
                    .build();

            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));
            when(repository.save(any(TaxBracket.class))).thenReturn(updatedBracket);

            TaxBracket result = service.updateIncomeTaxBracket(bracket.getId(), updateRequest);

            assertThat(result.getMinIncome()).isEqualTo(updateRequest.getMinIncome());
            assertThat(result.getMaxIncome()).isEqualTo(updateRequest.getMaxIncome());
            assertThat(result.getBaseTax()).isEqualTo(updateRequest.getBaseTax());
            assertThat(result.getMarginalRate()).isEqualTo(updateRequest.getMarginalRate());
            assertThat(result.getThreshold()).isEqualTo(updateRequest.getThreshold());
            assertThat(result.getEffectiveDate()).isEqualTo(updateRequest.getEffectiveDate());
        }

        @Test
        void shouldThrowNotFoundWhenBracketToUpdateDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateIncomeTaxBracket(unknownId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowNotFoundWhenBracketToUpdateIsSoftDeleted() {
            bracket.setDeletedAt(Instant.now());
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));

            assertThatThrownBy(() -> service.updateIncomeTaxBracket(bracket.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }
    }

    @Nested
    class DeleteTaxBracketTests {

        @Test
        void shouldSoftDeleteBracketBySettingDeletedAt() {
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));
            when(repository.save(any(TaxBracket.class))).thenReturn(bracket);

            service.deleteIncomeTaxBracket(bracket.getId());

            assertThat(bracket.getDeletedAt()).isNotNull();
            verify(repository).save(bracket);
        }

        @Test
        void shouldThrowNotFoundWhenBracketToDeleteDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(repository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteIncomeTaxBracket(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowNotFoundWhenBracketToDeleteIsAlreadySoftDeleted() {
            bracket.setDeletedAt(Instant.now());
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));

            assertThatThrownBy(() -> service.deleteIncomeTaxBracket(bracket.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));

            verify(repository, never()).save(any());
        }
    }
}

