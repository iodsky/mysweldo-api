package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitService;
import com.iodsky.mysweldo.contribution.Contribution;
import com.iodsky.mysweldo.contribution.ContributionService;
import com.iodsky.mysweldo.deduction.Deduction;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.pagIbig.PagibigRate;
import com.iodsky.mysweldo.pagIbig.PagibigRateRepository;
import com.iodsky.mysweldo.payroll.calc.*;
import com.iodsky.mysweldo.payroll.item.*;
import com.iodsky.mysweldo.philhealth.PhilhealthRate;
import com.iodsky.mysweldo.philhealth.PhilhealthRateRepository;
import com.iodsky.mysweldo.sss.SssRate;
import com.iodsky.mysweldo.sss.SssRateRepository;
import com.iodsky.mysweldo.tax.TaxBracket;
import com.iodsky.mysweldo.tax.TaxBracketRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTest {

    @InjectMocks
    private PayrollRunService service;

    @Mock private PayrollRunRepository repository;
    @Mock private PayrollRunMapper mapper;
    @Mock private EmployeeService employeeService;
    @Mock private AttendanceService attendanceService;
    @Mock private PayrollItemRepository payrollItemRepository;
    @Mock private PayrollItemAssembler payrollItemAssembler;
    @Mock private PayrollItemMapper payrollItemMapper;
    @Mock private DeductionService deductionService;
    @Mock private ContributionService contributionService;
    @Mock private BenefitService benefitService;
    @Mock private PhilhealthRateRepository philhealthRateRepository;
    @Mock private PagibigRateRepository pagibigRateRepository;
    @Mock private SssRateRepository sssRateRepository;
    @Mock private TaxBracketRepository taxBracketRepository;

    private PayrollRun draftRun(UUID id) {
        return PayrollRun.builder()
                .id(id)
                .status(PayrollRunStatus.DRAFT)
                .period(PayrollPeriod.of(
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 3, 15),
                        PayrollFrequency.SEMI_MONTHLY))
                .type(PayrollRunType.REGULAR)
                .build();
    }

    private void stubStatutoryRates() {
        when(philhealthRateRepository.findLatestByEffectiveDate(any())).thenReturn(Optional.of(mock(PhilhealthRate.class)));
        when(pagibigRateRepository.findLatestByEffectiveDate(any())).thenReturn(Optional.of(mock(PagibigRate.class)));
        when(sssRateRepository.findLatestByEffectiveDate(any())).thenReturn(Optional.of(mock(SssRate.class)));
        when(taxBracketRepository.findAllByLatestEffectiveDate(any())).thenReturn(List.of(mock(TaxBracket.class)));
        when(deductionService.getDeductionByCode("SSS")).thenReturn(mock(Deduction.class));
        when(deductionService.getDeductionByCode("PHIC")).thenReturn(mock(Deduction.class));
        when(deductionService.getDeductionByCode("HDMF")).thenReturn(mock(Deduction.class));
        when(deductionService.getDeductionByCode("TAX")).thenReturn(mock(Deduction.class));
        when(contributionService.getContributionByCode("SSS_ER")).thenReturn(mock(Contribution.class));
        when(contributionService.getContributionByCode("PHIC_ER")).thenReturn(mock(Contribution.class));
        when(contributionService.getContributionByCode("HDMF_ER")).thenReturn(mock(Contribution.class));
    }

    @Nested
    class CreatePayrollRunTests {

        @Test
        void shouldCreatePayrollRunInDraftStatusWhenPeriodIsValid() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 1));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 15));
            request.setPayrollFrequency(PayrollFrequency.SEMI_MONTHLY);
            request.setType(PayrollRunType.REGULAR);
            request.setNotes("March payroll");

            PayrollRunDto expectedDto = PayrollRunDto.builder().status(PayrollRunStatus.DRAFT).build();
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(expectedDto);

            PayrollRunDto result = service.createPayrollRun(request);

            assertThat(result.getStatus()).isEqualTo(PayrollRunStatus.DRAFT);
            verify(repository).save(any(PayrollRun.class));
        }

        @Test
        void shouldThrowBadRequestWhenPeriodEndDateIsBeforeStartDate() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 31));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 1));
            request.setPayrollFrequency(PayrollFrequency.SEMI_MONTHLY);
            request.setType(PayrollRunType.REGULAR);

            assertThatThrownBy(() -> service.createPayrollRun(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrowBadRequestWhenPeriodDurationDoesNotMatchFrequency() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 1));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 31));
            request.setPayrollFrequency(PayrollFrequency.SEMI_MONTHLY);
            request.setType(PayrollRunType.REGULAR);

            assertThatThrownBy(() -> service.createPayrollRun(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrowConflictWhenAnOverlappingRegularRunExists() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 1));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 15));
            request.setPayrollFrequency(PayrollFrequency.SEMI_MONTHLY);
            request.setType(PayrollRunType.REGULAR);

            when(repository.existsOverlappingByType(
                    eq(PayrollRunType.REGULAR),
                    eq(LocalDate.of(2025, 3, 1)),
                    eq(LocalDate.of(2025, 3, 15))))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.createPayrollRun(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);

            verify(repository, never()).save(any(PayrollRun.class));
        }

        @Test
        void shouldAllowOffCycleRunThatOverlapsARegularRun() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 1));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 15));
            request.setPayrollFrequency(PayrollFrequency.SEMI_MONTHLY);
            request.setType(PayrollRunType.OFF_CYCLE);

            PayrollRunDto expectedDto = PayrollRunDto.builder().status(PayrollRunStatus.DRAFT).build();
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(expectedDto);

            service.createPayrollRun(request);

            verify(repository, never()).existsOverlappingByType(any(), any(), any());
            verify(repository).save(any(PayrollRun.class));
        }
    }

    @Nested
    class GeneratePayrollTests {

        @Test
        void shouldGeneratePayrollForProvidedEmployeeIds() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L, 2L));

            PayrollItem item1 = PayrollItem.builder().grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(20000)).build();
            PayrollItem item2 = PayrollItem.builder().grossPay(BigDecimal.valueOf(18000))
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(18000)).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(eq(runId), anyLong())).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(true);
            when(attendanceService.hasAttendance(eq(2L), any(), any())).thenReturn(true);
            stubStatutoryRates();
            when(payrollItemAssembler.buildPayroll(eq(1L), eq(run), any())).thenReturn(item1);
            when(payrollItemAssembler.buildPayroll(eq(2L), eq(run), any())).thenReturn(item2);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item1, item2));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().id(runId).build());

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertThat(response.getPayrollRun().getId()).isEqualTo(runId);
            assertThat(response.getSkippedEmployeeIds()).isNullOrEmpty();
        }

        @Test
        void shouldFallBackToAllActiveEmployeesWhenEmployeeIdsIsNull() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(null);

            PayrollItem item = PayrollItem.builder().grossPay(BigDecimal.TEN)
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.TEN).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(List.of(10L));
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 10L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(10L), any(), any())).thenReturn(true);
            stubStatutoryRates();
            when(payrollItemAssembler.buildPayroll(eq(10L), eq(run), any())).thenReturn(item);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            assertThatNoException().isThrownBy(() -> service.generatePayroll(runId, request));
            verify(employeeService).getAllActiveEmployeeIds();
        }

        @Test
        void shouldFallBackToAllActiveEmployeesWhenEmployeeIdsIsEmpty() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(Collections.emptyList());

            PayrollItem item = PayrollItem.builder().grossPay(BigDecimal.TEN)
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.TEN).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(List.of(10L));
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 10L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(10L), any(), any())).thenReturn(true);
            stubStatutoryRates();
            when(payrollItemAssembler.buildPayroll(eq(10L), eq(run), any())).thenReturn(item);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            assertThatNoException().isThrownBy(() -> service.generatePayroll(runId, request));
            verify(employeeService).getAllActiveEmployeeIds();
        }

        @Test
        void shouldSkipEmployeesWhoAlreadyHaveAPayrollItemForTheRun() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L, 2L));

            PayrollItem item2 = PayrollItem.builder().grossPay(BigDecimal.valueOf(18000))
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(18000)).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            stubStatutoryRates();
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(true);
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 2L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(2L), any(), any())).thenReturn(true);
            when(payrollItemAssembler.buildPayroll(eq(2L), eq(run), any())).thenReturn(item2);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item2));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertThat(response.getSkippedEmployeeIds()).contains(1L);
            assertThat(response.getSkippedEmployeeIds()).doesNotContain(2L);
        }

        @Test
        void shouldSkipEmployeesWithNoAttendanceInThePeriod() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            stubStatutoryRates();
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(false);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(Collections.emptyList());
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertThat(response.getSkippedEmployeeIds()).contains(1L);
        }

        @Test
        void shouldAggregateRunTotalsAfterGeneratingItems() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            EmployerContribution ec = EmployerContribution.builder().amount(BigDecimal.valueOf(1500)).build();
            PayrollItem item = PayrollItem.builder()
                    .grossPay(BigDecimal.valueOf(30000))
                    .totalBenefits(BigDecimal.valueOf(1000))
                    .totalDeductions(BigDecimal.valueOf(2000))
                    .netPay(BigDecimal.valueOf(29000))
                    .employerContributions(List.of(ec))
                    .build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            stubStatutoryRates();
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(true);
            when(payrollItemAssembler.buildPayroll(eq(1L), eq(run), any())).thenReturn(item);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            service.generatePayroll(runId, request);

            assertThat(run.getTotalGrossPay()).isEqualByComparingTo(BigDecimal.valueOf(30000));
            assertThat(run.getTotalBenefits()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(run.getTotalDeductions()).isEqualByComparingTo(BigDecimal.valueOf(2000));
            assertThat(run.getTotalNetPay()).isEqualByComparingTo(BigDecimal.valueOf(29000));
            assertThat(run.getTotalEmployerCost()).isEqualByComparingTo(BigDecimal.valueOf(1500));
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExist() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.generatePayroll(runId, new GeneratePayrollRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotInDraftStatus() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.APPROVED);

            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.generatePayroll(runId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrowBadRequestWhenNoActiveEmployeesExistAfterFallback() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(null);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> service.generatePayroll(runId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class UpdatePayrollRunStatusTests {

        @Test
        void shouldTransitionFromDraftToApproved() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            when(repository.findById(id)).thenReturn(Optional.of(run));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().status(PayrollRunStatus.APPROVED).build());

            PayrollRunDto result = service.updatePayrollRunStatus(id, PayrollRunStatus.APPROVED);

            assertThat(run.getStatus()).isEqualTo(PayrollRunStatus.APPROVED);
            verify(repository).save(run);
            assertThat(result.getStatus()).isEqualTo(PayrollRunStatus.APPROVED);
        }

        @Test
        void shouldTransitionFromApprovedToProcessed() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            run.setStatus(PayrollRunStatus.APPROVED);
            when(repository.findById(id)).thenReturn(Optional.of(run));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().status(PayrollRunStatus.PROCESSED).build());

            service.updatePayrollRunStatus(id, PayrollRunStatus.PROCESSED);

            assertThat(run.getStatus()).isEqualTo(PayrollRunStatus.PROCESSED);
            verify(repository).save(run);
        }

        @Test
        void shouldThrowBadRequestWhenTransitioningFromDraftToProcessed() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            when(repository.findById(id)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.updatePayrollRunStatus(id, PayrollRunStatus.PROCESSED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequestWhenTransitioningFromApprovedToDraft() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            run.setStatus(PayrollRunStatus.APPROVED);
            when(repository.findById(id)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.updatePayrollRunStatus(id, PayrollRunStatus.DRAFT))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsAlreadyProcessed() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            run.setStatus(PayrollRunStatus.PROCESSED);
            when(repository.findById(id)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.updatePayrollRunStatus(id, PayrollRunStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(repository, never()).save(any());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePayrollRunStatus(id, PayrollRunStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetAllPayrollRunsTests {

        @Test
        void shouldReturnAllPayrollRunsWhenNoFiltersProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));
            when(repository.findAll(any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, null, null, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldFilterByDateRangeWhenBothDatesProvided() {
            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByPeriod_StartDateGreaterThanEqualAndPeriod_EndDateLessThanEqual(
                    eq(start), eq(end), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(start, end, null, null, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldFilterByTypeAndStatusWhenBothAreProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByTypeAndStatus(eq(PayrollRunType.REGULAR), eq(PayrollRunStatus.DRAFT), any(Pageable.class)))
                    .thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, PayrollRunType.REGULAR, PayrollRunStatus.DRAFT, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldFilterByTypeOnlyWhenOnlyTypeIsProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByType(eq(PayrollRunType.REGULAR), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, PayrollRunType.REGULAR, null, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldFilterByStatusOnlyWhenOnlyStatusIsProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByStatus(eq(PayrollRunStatus.DRAFT), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, null, PayrollRunStatus.DRAFT, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void shouldReturnEmptyPageWhenNoRunsMatchFilters() {
            when(repository.getAllByStatus(eq(PayrollRunStatus.PROCESSED), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, null, PayrollRunStatus.PROCESSED, 0, 10);

            assertThat(result.isEmpty()).isTrue();
        }
    }

    @Nested
    class GetPayrollRunByIdTests {

        @Test
        void shouldReturnDtoWhenPayrollRunExists() {
            UUID id = UUID.randomUUID();
            PayrollRun run = draftRun(id);
            PayrollRunDto dto = PayrollRunDto.builder().id(id).build();

            when(repository.findById(id)).thenReturn(Optional.of(run));
            when(mapper.toDto(run)).thenReturn(dto);

            PayrollRunDto result = service.getPayrollRunById(id);

            assertThat(result.getId()).isEqualTo(id);
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPayrollRunById(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetPayrollItemsTests {

        @Test
        void shouldReturnPaginatedPayrollItemsForARun() {
            UUID runId = UUID.randomUUID();
            PayrollItem item = PayrollItem.builder().build();
            Page<PayrollItem> itemPage = new PageImpl<>(List.of(item));
            PayrollItemDto dto = PayrollItemDto.builder().build();

            when(payrollItemRepository.findAllByPayrollRun_Id(eq(runId), any(Pageable.class))).thenReturn(itemPage);
            when(payrollItemMapper.toDto(item)).thenReturn(dto);

            Page<PayrollItemDto> result = service.getPayrollItems(runId, 0, 10);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    class GetPayrollItemTests {

        @Test
        void shouldReturnPayrollItemDtoWhenFound() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollItem item = PayrollItem.builder().id(itemId).build();
            PayrollItemDto dto = PayrollItemDto.builder().id(itemId).build();

            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemMapper.toDto(item)).thenReturn(dto);

            PayrollItemDto result = service.getPayrollItem(runId, itemId);

            assertThat(result.getId()).isEqualTo(itemId);
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForRun() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getPayrollItem(runId, itemId))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class DeletePayrollItemTests {

        @Test
        void shouldDeleteItemAndRecomputeRunTotalsWhenRunIsDraft() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            PayrollItem item = PayrollItem.builder().id(itemId).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(Collections.emptyList());

            service.deletePayrollItem(runId, itemId);

            verify(payrollItemRepository).delete(item);
            verify(repository).save(run);
        }

        @Test
        void shouldThrowBadRequestWhenRunIsNotDraft() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.APPROVED);
            when(repository.findById(runId)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.deletePayrollItem(runId, UUID.randomUUID()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(payrollItemRepository, never()).delete(any());
        }

        @Test
        void shouldThrowNotFoundWhenItemDoesNotExistInRun() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletePayrollItem(runId, itemId))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(payrollItemRepository, never()).delete(any());
        }
    }

    @Nested
    class UpdatePayrollDeductionsTests {

        private PayrollItem itemWithDeductions(String code, BigDecimal amount) {
            Deduction deduction = Deduction.builder().code(code).build();
            PayrollDeduction pd = PayrollDeduction.builder().deduction(deduction).amount(amount).build();
            List<PayrollDeduction> deductions = new ArrayList<>(List.of(pd));
            return PayrollItem.builder()
                    .deductions(deductions)
                    .benefits(new ArrayList<>())
                    .grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.ZERO)
                    .totalDeductions(amount)
                    .netPay(BigDecimal.valueOf(20000).subtract(amount))
                    .build();
        }

        @Test
        void shouldOverrideAmountWhenDeductionCodeAlreadyExists() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            PayrollItem item = itemWithDeductions("SSS", BigDecimal.valueOf(500));

            LineItemRequest entry = new LineItemRequest();
            entry.setCode("SSS");
            entry.setAmount(BigDecimal.valueOf(800));

            UpdatePayrollDeductionRequest request = new UpdatePayrollDeductionRequest();
            request.setDeductions(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollDeductions(runId, itemId, request);

            assertThat(item.getDeductions().getFirst().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(800));
        }

        @Test
        void shouldAddNewDeductionWhenCodeDoesNotExist() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            PayrollItem item = PayrollItem.builder()
                    .deductions(new ArrayList<>())
                    .benefits(new ArrayList<>())
                    .grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.ZERO)
                    .totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(20000))
                    .build();

            Deduction deduction = Deduction.builder().code("PAGIBIG").build();
            LineItemRequest entry = new LineItemRequest();
            entry.setCode("PAGIBIG");
            entry.setAmount(BigDecimal.valueOf(200));

            UpdatePayrollDeductionRequest request = new UpdatePayrollDeductionRequest();
            request.setDeductions(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(deductionService.getDeductionByCode("PAGIBIG")).thenReturn(deduction);
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollDeductions(runId, itemId, request);

            assertThat(item.getDeductions()).hasSize(1);
            assertThat(item.getDeductions().getFirst().getDeduction().getCode()).isEqualTo("PAGIBIG");
        }

        @Test
        void shouldRecalculateTotalDeductionsAndNetPayAfterUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            PayrollItem item = itemWithDeductions("SSS", BigDecimal.valueOf(500));
            item.setGrossPay(BigDecimal.valueOf(20000));
            item.setTotalBenefits(BigDecimal.ZERO);

            LineItemRequest entry = new LineItemRequest();
            entry.setCode("SSS");
            entry.setAmount(BigDecimal.valueOf(1000));

            UpdatePayrollDeductionRequest request = new UpdatePayrollDeductionRequest();
            request.setDeductions(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollDeductions(runId, itemId, request);

            assertThat(item.getTotalDeductions()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(item.getNetPay()).isEqualByComparingTo(BigDecimal.valueOf(19000));
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExistForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePayrollDeductions(runId, UUID.randomUUID(), new UpdatePayrollDeductionRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePayrollDeductions(runId, itemId, new UpdatePayrollDeductionRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotDraftForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.APPROVED);

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.updatePayrollDeductions(runId, UUID.randomUUID(), new UpdatePayrollDeductionRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class UpdatePayrollBenefitsTests {

        @Test
        void shouldOverrideAmountWhenBenefitCodeAlreadyExists() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            Benefit benefit = Benefit.builder().code("RICE").build();
            PayrollBenefit pb = PayrollBenefit.builder().benefit(benefit).amount(BigDecimal.valueOf(500)).build();
            PayrollItem item = PayrollItem.builder()
                    .benefits(new ArrayList<>(List.of(pb)))
                    .deductions(new ArrayList<>())
                    .grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.valueOf(500))
                    .totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(20500))
                    .build();

            LineItemRequest entry = new LineItemRequest();
            entry.setCode("RICE");
            entry.setAmount(BigDecimal.valueOf(1000));

            UpdatePayrollBenefitRequest request = new UpdatePayrollBenefitRequest();
            request.setBenefits(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollBenefits(runId, itemId, request);

            assertThat(item.getBenefits().get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        }

        @Test
        void shouldAddNewBenefitWhenCodeDoesNotExist() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            PayrollItem item = PayrollItem.builder()
                    .benefits(new ArrayList<>())
                    .deductions(new ArrayList<>())
                    .grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.ZERO)
                    .totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(20000))
                    .build();

            Benefit benefit = Benefit.builder().code("TRANSPORT").build();
            LineItemRequest entry = new LineItemRequest();
            entry.setCode("TRANSPORT");
            entry.setAmount(BigDecimal.valueOf(300));

            UpdatePayrollBenefitRequest request = new UpdatePayrollBenefitRequest();
            request.setBenefits(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(benefitService.getBenefitByCode("TRANSPORT")).thenReturn(benefit);
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollBenefits(runId, itemId, request);

            assertThat(item.getBenefits()).hasSize(1);
            assertThat(item.getBenefits().get(0).getBenefit().getCode()).isEqualTo("TRANSPORT");
        }

        @Test
        void shouldRecalculateTotalBenefitsAndNetPayAfterUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            Benefit benefit = Benefit.builder().code("RICE").build();
            PayrollBenefit pb = PayrollBenefit.builder().benefit(benefit).amount(BigDecimal.valueOf(500)).build();
            PayrollItem item = PayrollItem.builder()
                    .benefits(new ArrayList<>(List.of(pb)))
                    .deductions(new ArrayList<>())
                    .grossPay(BigDecimal.valueOf(20000))
                    .totalBenefits(BigDecimal.valueOf(500))
                    .totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.valueOf(20500))
                    .build();

            LineItemRequest entry = new LineItemRequest();
            entry.setCode("RICE");
            entry.setAmount(BigDecimal.valueOf(1500));

            UpdatePayrollBenefitRequest request = new UpdatePayrollBenefitRequest();
            request.setBenefits(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollBenefits(runId, itemId, request);

            assertThat(item.getTotalBenefits()).isEqualByComparingTo(BigDecimal.valueOf(1500));
            assertThat(item.getNetPay()).isEqualByComparingTo(BigDecimal.valueOf(21500));
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExistForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePayrollBenefits(runId, UUID.randomUUID(), new UpdatePayrollBenefitRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updatePayrollBenefits(runId, itemId, new UpdatePayrollBenefitRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotDraftForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.PROCESSED);

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            assertThatThrownBy(() -> service.updatePayrollBenefits(runId, UUID.randomUUID(), new UpdatePayrollBenefitRequest()))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
