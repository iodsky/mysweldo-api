package com.iodsky.mysweldo.payroll.tax;

import com.iodsky.mysweldo.tax.IncomeTaxBracket;
import com.iodsky.mysweldo.tax.IncomeTaxBracketRepository;
import com.iodsky.mysweldo.tax.IncomeTaxBracketRequest;
import com.iodsky.mysweldo.tax.IncomeTaxBracketService;
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
class IncomeTaxBracketServiceTest {

    @InjectMocks
    private IncomeTaxBracketService service;

    @Mock
    private IncomeTaxBracketRepository repository;

    private IncomeTaxBracket bracket;
    private IncomeTaxBracketRequest request;

    @BeforeEach
    void setUp() {
        bracket = IncomeTaxBracket.builder()
                .id(UUID.randomUUID())
                .minIncome(new BigDecimal("20833.00"))
                .maxIncome(new BigDecimal("33332.00"))
                .baseTax(new BigDecimal("0.00"))
                .marginalRate(new BigDecimal("0.2000"))
                .threshold(new BigDecimal("20833.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        request = IncomeTaxBracketRequest.builder()
                .minIncome(new BigDecimal("20833.00"))
                .maxIncome(new BigDecimal("33332.00"))
                .baseTax(new BigDecimal("0.00"))
                .marginalRate(new BigDecimal("0.2000"))
                .threshold(new BigDecimal("20833.00"))
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Nested
    class CreateIncomeTaxBracketTests {

        @Test
        void shouldReturnSavedBracketWhenAllFieldsAreProvided() {
            when(repository.save(any(IncomeTaxBracket.class))).thenReturn(bracket);

            IncomeTaxBracket result = service.createIncomeTaxBracket(request);

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
            IncomeTaxBracketRequest openEndedRequest = IncomeTaxBracketRequest.builder()
                    .minIncome(new BigDecimal("666667.00"))
                    .maxIncome(null)
                    .baseTax(new BigDecimal("130833.33"))
                    .marginalRate(new BigDecimal("0.3500"))
                    .threshold(new BigDecimal("666667.00"))
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .build();

            IncomeTaxBracket openEndedBracket = IncomeTaxBracket.builder()
                    .id(UUID.randomUUID())
                    .minIncome(openEndedRequest.getMinIncome())
                    .maxIncome(null)
                    .baseTax(openEndedRequest.getBaseTax())
                    .marginalRate(openEndedRequest.getMarginalRate())
                    .threshold(openEndedRequest.getThreshold())
                    .effectiveDate(openEndedRequest.getEffectiveDate())
                    .build();

            when(repository.save(any(IncomeTaxBracket.class))).thenReturn(openEndedBracket);

            IncomeTaxBracket result = service.createIncomeTaxBracket(openEndedRequest);

            assertThat(result.getMaxIncome()).isNull();
        }

        @Test
        void shouldPersistBracketWithZeroBaseTaxForFirstBracket() {
            IncomeTaxBracketRequest firstBracketRequest = IncomeTaxBracketRequest.builder()
                    .minIncome(BigDecimal.ZERO)
                    .maxIncome(new BigDecimal("20832.00"))
                    .baseTax(BigDecimal.ZERO)
                    .marginalRate(BigDecimal.ZERO)
                    .threshold(BigDecimal.ZERO)
                    .effectiveDate(LocalDate.of(2024, 1, 1))
                    .build();

            IncomeTaxBracket zeroBaseTaxBracket = IncomeTaxBracket.builder()
                    .id(UUID.randomUUID())
                    .minIncome(BigDecimal.ZERO)
                    .baseTax(BigDecimal.ZERO)
                    .effectiveDate(firstBracketRequest.getEffectiveDate())
                    .build();

            when(repository.save(any(IncomeTaxBracket.class))).thenReturn(zeroBaseTaxBracket);

            IncomeTaxBracket result = service.createIncomeTaxBracket(firstBracketRequest);

            assertThat(result.getBaseTax()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldPropagateExceptionWhenRepositorySaveFails() {
            when(repository.save(any(IncomeTaxBracket.class)))
                    .thenThrow(new RuntimeException("Database error"));

            assertThatThrownBy(() -> service.createIncomeTaxBracket(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Database error");
        }
    }

    @Nested
    class GetAllIncomeTaxBracketsTests {

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedBracketsWhenNoFiltersApplied() {
            Page<IncomeTaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(0, 10, null, null, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(bracket);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByEffectiveDateWhenDateProvided() {
            Page<IncomeTaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, LocalDate.of(2024, 1, 1), null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByMinIncomeWhenMinIncomeProvided() {
            Page<IncomeTaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, new BigDecimal("20833.00"), null);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnBracketsFilteredByMaxIncomeWhenMaxIncomeIsGreaterThanZero() {
            Page<IncomeTaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, null, new BigDecimal("33332.00"));

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldIgnoreMaxIncomeFilterWhenMaxIncomeIsZero() {
            Page<IncomeTaxBracket> expectedPage = new PageImpl<>(List.of(bracket));
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, null, null, BigDecimal.ZERO);

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnEmptyPageWhenNoBracketsMatchFilters() {
            Page<IncomeTaxBracket> emptyPage = new PageImpl<>(List.of());
            when(repository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<IncomeTaxBracket> result = service.getAllIncomeTaxBrackets(
                    0, 10, LocalDate.of(2099, 1, 1), null, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetAllIncomeTaxBracketsByDateTests {

        @Test
        void shouldReturnBracketsMatchingGivenEffectiveDate() {
            LocalDate date = LocalDate.of(2024, 1, 1);
            when(repository.findAllByEffectiveDate(date)).thenReturn(List.of(bracket));

            List<IncomeTaxBracket> result = service.getAllIncomeTaxBracketsByDate(date);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getEffectiveDate()).isEqualTo(date);
        }

        @Test
        void shouldReturnEmptyListWhenNoBracketsExistForGivenDate() {
            LocalDate date = LocalDate.of(2099, 1, 1);
            when(repository.findAllByEffectiveDate(date)).thenReturn(List.of());

            List<IncomeTaxBracket> result = service.getAllIncomeTaxBracketsByDate(date);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class GetIncomeTaxBracketByIdTests {

        @Test
        void shouldReturnBracketWhenItExistsAndIsNotDeleted() {
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));

            IncomeTaxBracket result = service.getIncomeTaxBracketById(bracket.getId());

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
    class GetIncomeTaxBracketByIncomeAndDateTests {

        @Test
        void shouldReturnMatchingBracketForGivenIncomeAndDate() {
            BigDecimal income = new BigDecimal("25000.00");
            LocalDate date = LocalDate.of(2024, 1, 1);
            when(repository.findByIncomeAndEffectiveDate(income, date)).thenReturn(bracket);

            IncomeTaxBracket result = service.getIncomeTaxBracketByIncomeAndDate(income, date);

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
    class UpdateIncomeTaxBracketTests {

        @Test
        void shouldUpdateAllFieldsAndReturnUpdatedBracket() {
            IncomeTaxBracketRequest updateRequest = IncomeTaxBracketRequest.builder()
                    .minIncome(new BigDecimal("33333.00"))
                    .maxIncome(new BigDecimal("66666.00"))
                    .baseTax(new BigDecimal("2500.00"))
                    .marginalRate(new BigDecimal("0.2500"))
                    .threshold(new BigDecimal("33333.00"))
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            IncomeTaxBracket updatedBracket = IncomeTaxBracket.builder()
                    .id(bracket.getId())
                    .minIncome(updateRequest.getMinIncome())
                    .maxIncome(updateRequest.getMaxIncome())
                    .baseTax(updateRequest.getBaseTax())
                    .marginalRate(updateRequest.getMarginalRate())
                    .threshold(updateRequest.getThreshold())
                    .effectiveDate(updateRequest.getEffectiveDate())
                    .build();

            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));
            when(repository.save(any(IncomeTaxBracket.class))).thenReturn(updatedBracket);

            IncomeTaxBracket result = service.updateIncomeTaxBracket(bracket.getId(), updateRequest);

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
    class DeleteIncomeTaxBracketTests {

        @Test
        void shouldSoftDeleteBracketBySettingDeletedAt() {
            when(repository.findById(bracket.getId())).thenReturn(Optional.of(bracket));
            when(repository.save(any(IncomeTaxBracket.class))).thenReturn(bracket);

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

