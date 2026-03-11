package com.iodsky.mysweldo.payroll.contribution.sss;

import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.sss.SssRateRepository;
import com.iodsky.mysweldo.sss.SssRateRequest;
import com.iodsky.mysweldo.sss.SssRateService;
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
class SssRateServiceTest {

    @InjectMocks
    private SssRateService service;

    @Mock
    private SssRateRepository sssRateTableRepository;

    private SssRate rateTable;
    private SssRateRequest request;

    @BeforeEach
    void setUp() {
        List<SssRate.SalaryBracket> brackets = List.of(
                SssRate.SalaryBracket.builder()
                        .minSalary(new BigDecimal("0.00"))
                        .maxSalary(new BigDecimal("4999.99"))
                        .msc(new BigDecimal("5000.00"))
                        .build(),
                SssRate.SalaryBracket.builder()
                        .minSalary(new BigDecimal("5000.00"))
                        .maxSalary(null)
                        .msc(new BigDecimal("20000.00"))
                        .build()
        );

        rateTable = SssRate.builder()
                .id(UUID.randomUUID())
                .totalSss(new BigDecimal("1800.00"))
                .employeeRate(new BigDecimal("0.0450"))
                .employerRate(new BigDecimal("0.0950"))
                .salaryBrackets(brackets)
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();

        List<SssRateRequest.SalaryBracketRequest> bracketRequests = List.of(
                SssRateRequest.SalaryBracketRequest.builder()
                        .minSalary(new BigDecimal("0.00"))
                        .maxSalary(new BigDecimal("4999.99"))
                        .msc(new BigDecimal("5000.00"))
                        .build(),
                SssRateRequest.SalaryBracketRequest.builder()
                        .minSalary(new BigDecimal("5000.00"))
                        .maxSalary(null)
                        .msc(new BigDecimal("20000.00"))
                        .build()
        );

        request = SssRateRequest.builder()
                .totalSss(new BigDecimal("1800.00"))
                .employeeRate(new BigDecimal("0.0450"))
                .employerRate(new BigDecimal("0.0950"))
                .salaryBrackets(bracketRequests)
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .build();
    }

    @Nested
    class CreateSssRateTests {

        @Test
        void shouldReturnSavedRateTableWhenValidRequestProvided() {
            when(sssRateTableRepository.save(any(SssRate.class))).thenReturn(rateTable);

            SssRate result = service.createSssRateTable(request);

            assertThat(result).isNotNull();
            assertThat(result.getTotalSss()).isEqualTo(request.getTotalSss());
            assertThat(result.getEmployeeRate()).isEqualTo(request.getEmployeeRate());
            assertThat(result.getEmployerRate()).isEqualTo(request.getEmployerRate());
            assertThat(result.getEffectiveDate()).isEqualTo(request.getEffectiveDate());
        }

        @Test
        void shouldMapSalaryBracketsCorrectlyFromRequest() {
            when(sssRateTableRepository.save(any(SssRate.class))).thenAnswer(inv -> inv.getArgument(0));

            SssRate result = service.createSssRateTable(request);

            assertThat(result.getSalaryBrackets()).hasSize(2);
            assertThat(result.getSalaryBrackets().get(0).getMinSalary()).isEqualTo(new BigDecimal("0.00"));
            assertThat(result.getSalaryBrackets().get(0).getMaxSalary()).isEqualTo(new BigDecimal("4999.99"));
            assertThat(result.getSalaryBrackets().get(0).getMsc()).isEqualTo(new BigDecimal("5000.00"));
        }

        @Test
        void shouldSaveRateTableWithEmptySalaryBracketsWhenNoneProvided() {
            request.setSalaryBrackets(List.of());
            when(sssRateTableRepository.save(any(SssRate.class))).thenAnswer(inv -> inv.getArgument(0));

            SssRate result = service.createSssRateTable(request);

            assertThat(result.getSalaryBrackets()).isEmpty();
        }
    }

    @Nested
    class GetAllSssRateTablesTests {

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnPaginatedRateTablesWhenNoEffectiveDateFilterProvided() {
            Page<SssRate> expectedPage = new PageImpl<>(List.of(rateTable));
            when(sssRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<SssRate> result = service.getAllSssRateTables(0, 10, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(rateTable);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnRateTablesFilteredByEffectiveDateWhenDateProvided() {
            Page<SssRate> expectedPage = new PageImpl<>(List.of(rateTable));
            when(sssRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<SssRate> result = service.getAllSssRateTables(0, 10, LocalDate.of(2024, 1, 1));

            assertThat(result.getContent()).hasSize(1);
        }

        @SuppressWarnings("unchecked")
        @Test
        void shouldReturnEmptyPageWhenNoRateTablesExist() {
            Page<SssRate> emptyPage = new PageImpl<>(List.of());
            when(sssRateTableRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<SssRate> result = service.getAllSssRateTables(0, 10, null);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class GetSssRateByIdTests {

        @Test
        void shouldReturnRateTableWhenItExistsAndIsNotDeleted() {
            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            SssRate result = service.getSssRateTableById(rateTable.getId());

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenRateTableDoesNotExist() {
            UUID unknownId = UUID.randomUUID();
            when(sssRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSssRateTableById(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(unknownId.toString());
                    });
        }

        @Test
        void shouldThrowNotFoundWhenRateTableIsSoftDeleted() {
            rateTable.setDeletedAt(Instant.now());
            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.getSssRateTableById(rateTable.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class GetSssRateBySalaryAndDateTests {

        @Test
        void shouldReturnRateTableWhenSalaryFallsWithinABracketForGivenDate() {
            LocalDate date = LocalDate.of(2024, 6, 1);
            BigDecimal salary = new BigDecimal("3000.00");
            when(sssRateTableRepository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.of(rateTable));

            SssRate result = service.getSssRateTableBySalaryAndDate(salary, date);

            assertThat(result).isEqualTo(rateTable);
        }

        @Test
        void shouldThrowNotFoundWhenNoRateTableExistsForGivenDate() {
            LocalDate date = LocalDate.of(2020, 1, 1);
            BigDecimal salary = new BigDecimal("3000.00");
            when(sssRateTableRepository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSssRateTableBySalaryAndDate(salary, date))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(date.toString());
                    });
        }

        @Test
        void shouldThrowNotFoundWhenSalaryDoesNotFallWithinAnyBracket() {
            LocalDate date = LocalDate.of(2024, 6, 1);
            BigDecimal salaryBelowAllBrackets = new BigDecimal("-1.00");
            when(sssRateTableRepository.findLatestByEffectiveDate(date))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.getSssRateTableBySalaryAndDate(salaryBelowAllBrackets, date))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains(salaryBelowAllBrackets.toString());
                    });
        }
    }

    @Nested
    class UpdateSssRateTests {

        @Test
        void shouldUpdateAllFieldsAndReturnUpdatedRateTableWhenExists() {
            List<SssRateRequest.SalaryBracketRequest> newBracketRequests = List.of(
                    SssRateRequest.SalaryBracketRequest.builder()
                            .minSalary(new BigDecimal("0.00"))
                            .maxSalary(new BigDecimal("9999.99"))
                            .msc(new BigDecimal("10000.00"))
                            .build()
            );

            SssRateRequest updateRequest = SssRateRequest.builder()
                    .totalSss(new BigDecimal("2400.00"))
                    .employeeRate(new BigDecimal("0.0500"))
                    .employerRate(new BigDecimal("0.1000"))
                    .salaryBrackets(newBracketRequests)
                    .effectiveDate(LocalDate.of(2025, 1, 1))
                    .build();

            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(sssRateTableRepository.save(any(SssRate.class))).thenAnswer(inv -> inv.getArgument(0));

            SssRate result = service.updateSssRateTable(rateTable.getId(), updateRequest);

            assertThat(result.getTotalSss()).isEqualTo(updateRequest.getTotalSss());
            assertThat(result.getEmployeeRate()).isEqualTo(updateRequest.getEmployeeRate());
            assertThat(result.getEmployerRate()).isEqualTo(updateRequest.getEmployerRate());
            assertThat(result.getEffectiveDate()).isEqualTo(updateRequest.getEffectiveDate());
            assertThat(result.getSalaryBrackets()).hasSize(1);
            assertThat(result.getSalaryBrackets().get(0).getMsc()).isEqualTo(new BigDecimal("10000.00"));
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingNonExistentRateTable() {
            UUID unknownId = UUID.randomUUID();
            when(sssRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateSssRateTable(unknownId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowNotFoundWhenUpdatingSoftDeletedRateTable() {
            rateTable.setDeletedAt(Instant.now());
            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.updateSssRateTable(rateTable.getId(), request))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    class DeleteSssRateTests {

        @Test
        void shouldSoftDeleteRateTableBySettingDeletedAtWhenExists() {
            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));
            when(sssRateTableRepository.save(any(SssRate.class))).thenAnswer(inv -> inv.getArgument(0));

            service.deleteSssRateTable(rateTable.getId());

            assertThat(rateTable.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrowNotFoundWhenDeletingNonExistentRateTable() {
            UUID unknownId = UUID.randomUUID();
            when(sssRateTableRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSssRateTable(unknownId))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }

        @Test
        void shouldThrowNotFoundWhenDeletingAlreadySoftDeletedRateTable() {
            rateTable.setDeletedAt(Instant.now());
            when(sssRateTableRepository.findById(rateTable.getId()))
                    .thenReturn(Optional.of(rateTable));

            assertThatThrownBy(() -> service.deleteSssRateTable(rateTable.getId()))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.NOT_FOUND));
        }
    }
}

