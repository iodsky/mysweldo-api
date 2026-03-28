package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.payroll.core.PayrollConfiguration;
import com.iodsky.mysweldo.benefit.BenefitService;
import com.iodsky.mysweldo.deduction.Deduction;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.payroll.core.*;
import com.iodsky.mysweldo.payroll.strategy.PayrollComputationStrategy;
import com.iodsky.mysweldo.payroll.strategy.PayrollStrategyFactory;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollRunServiceTest {

    @InjectMocks
    private PayrollRunService service;

    @Mock
    private PayrollRunRepository repository;

    @Mock
    private PayrollRunMapper mapper;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private AttendanceService attendanceService;

    @Mock
    private PayrollCalculator calculator;

    @Mock
    private PayrollItemRepository payrollItemRepository;

    @Mock
    private PayrollBuilder payrollBuilder;

    @Mock
    private PayrollItemMapper payrollItemMapper;

    @Mock
    private DeductionService deductionService;

    @Mock
    private BenefitService benefitService;

    @Mock
    private PayrollStrategyFactory strategyFactory;

    @Mock
    private PayrollComputationStrategy payrollComputationStrategy;

    private PayrollRun draftRun(UUID id) {
        return PayrollRun.builder()
                .id(id)
                .status(PayrollRunStatus.DRAFT)
                .periodStartDate(LocalDate.of(2025, 3, 1))
                .periodEndDate(LocalDate.of(2025, 3, 31))
                .type(PayrollRunType.REGULAR)
                .payrollFrequency(PayrollFrequency.SEMI_MONTHLY)
                .build();
    }

    @Nested
    class CreatePayrollRunTests {

        @Test
        void shouldCreatePayrollRunInDraftStatusWhenPeriodIsValid() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 1));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 31));
            request.setType(PayrollRunType.REGULAR);
            request.setNotes("March payroll");

            PayrollRunDto expectedDto = PayrollRunDto.builder().status(PayrollRunStatus.DRAFT).build();
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(expectedDto);

            PayrollRunDto result = service.createPayrollRun(request);

            assertEquals(PayrollRunStatus.DRAFT, result.getStatus());
            verify(repository).save(any(PayrollRun.class));
        }

        @Test
        void shouldCreatePayrollRunWhenStartDateEqualsEndDate() {
            PayrollRunRequest request = new PayrollRunRequest();
            LocalDate sameDay = LocalDate.of(2025, 3, 15);
            request.setPeriodStartDate(sameDay);
            request.setPeriodEndDate(sameDay);
            request.setType(PayrollRunType.REGULAR);

            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            assertDoesNotThrow(() -> service.createPayrollRun(request));
        }

        @Test
        void shouldThrowBadRequestWhenPeriodEndDateIsBeforeStartDate() {
            PayrollRunRequest request = new PayrollRunRequest();
            request.setPeriodStartDate(LocalDate.of(2025, 3, 31));
            request.setPeriodEndDate(LocalDate.of(2025, 3, 1));
            request.setType(PayrollRunType.REGULAR);

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.createPayrollRun(request)
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

    @Nested
    class GeneratePayrollTests {

        private PayrollConfiguration configuration() {
            return mock(PayrollConfiguration.class);
        }

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

            PayrollRunDto expectedDto = PayrollRunDto.builder().id(runId).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(eq(runId), anyLong())).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(true);
            when(attendanceService.hasAttendance(eq(2L), any(), any())).thenReturn(true);
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollBuilder.buildPayroll(eq(1L), eq(run), any())).thenReturn(item1);
            when(payrollBuilder.buildPayroll(eq(2L), eq(run), any())).thenReturn(item2);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item1, item2));
            when(mapper.toDto(run)).thenReturn(expectedDto);

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertEquals(runId, response.getPayrollRun().getId());
            assertTrue(response.getSkippedEmployeeIds() == null || response.getSkippedEmployeeIds().isEmpty());
        }

        @Test
        void shouldFallBackToAllActiveEmployeesWhenEmployeeIdsIsNull() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(null);

            List<Long> activeIds = List.of(10L);
            PayrollItem item = PayrollItem.builder().grossPay(BigDecimal.TEN)
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.TEN).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(activeIds);
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 10L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(10L), any(), any())).thenReturn(true);
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollBuilder.buildPayroll(eq(10L), eq(run), any())).thenReturn(item);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            assertDoesNotThrow(() -> service.generatePayroll(runId, request));
            verify(employeeService).getAllActiveEmployeeIds();
        }

        @Test
        void shouldFallBackToAllActiveEmployeesWhenEmployeeIdsIsEmpty() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(Collections.emptyList());

            List<Long> activeIds = List.of(10L);
            PayrollItem item = PayrollItem.builder().grossPay(BigDecimal.TEN)
                    .totalBenefits(BigDecimal.ZERO).totalDeductions(BigDecimal.ZERO)
                    .netPay(BigDecimal.TEN).build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(activeIds);
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 10L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(10L), any(), any())).thenReturn(true);
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            assertDoesNotThrow(() -> service.generatePayroll(runId, request));
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
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(true);
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 2L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(2L), any(), any())).thenReturn(true);
            when(payrollBuilder.buildPayroll(eq(2L), eq(run), any())).thenReturn(item2);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item2));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertTrue(response.getSkippedEmployeeIds().contains(1L));
            assertFalse(response.getSkippedEmployeeIds().contains(2L));
        }

        @Test
        void shouldSkipEmployeesWithNoAttendanceInThePeriod() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(false);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(Collections.emptyList());
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            GeneratePayrollResponse response = service.generatePayroll(runId, request);

            assertTrue(response.getSkippedEmployeeIds().contains(1L));
        }

        @Test
        void shouldAggregateRunTotalsAfterGeneratingItems() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            EmployerContribution ec = EmployerContribution.builder()
                    .amount(BigDecimal.valueOf(1500))
                    .build();

            PayrollItem item = PayrollItem.builder()
                    .grossPay(BigDecimal.valueOf(30000))
                    .totalBenefits(BigDecimal.valueOf(1000))
                    .totalDeductions(BigDecimal.valueOf(2000))
                    .netPay(BigDecimal.valueOf(29000))
                    .employerContributions(List.of(ec))
                    .build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(calculator.loadConfiguration(any())).thenReturn(configuration());
            when(payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(runId, 1L)).thenReturn(false);
            when(attendanceService.hasAttendance(eq(1L), any(), any())).thenReturn(true);
            when(payrollBuilder.buildPayroll(eq(1L), eq(run), any())).thenReturn(item);
            when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));
            when(mapper.toDto(run)).thenReturn(PayrollRunDto.builder().build());

            service.generatePayroll(runId, request);

            assertEquals(BigDecimal.valueOf(30000), run.getTotalGrossPay());
            assertEquals(BigDecimal.valueOf(1000), run.getTotalBenefits());
            assertEquals(BigDecimal.valueOf(2000), run.getTotalDeductions());
            assertEquals(BigDecimal.valueOf(29000), run.getTotalNetPay());
            assertEquals(BigDecimal.valueOf(1500), run.getTotalEmployerCost());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExist() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.generatePayroll(runId, new GeneratePayrollRequest())
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotInDraftStatus() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.APPROVED);

            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(List.of(1L));

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.generatePayroll(runId, request)
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenNoActiveEmployeesExistAfterFallback() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            GeneratePayrollRequest request = new GeneratePayrollRequest();
            request.setEmployeeIds(null);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(employeeService.getAllActiveEmployeeIds()).thenReturn(Collections.emptyList());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.generatePayroll(runId, request)
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
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

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldFilterByDateRangeWhenBothDatesProvided() {
            LocalDate start = LocalDate.of(2025, 3, 1);
            LocalDate end = LocalDate.of(2025, 3, 31);
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByPeriodStartDateGreaterThanEqualAndPeriodEndDateLessThanEqual(
                    eq(start), eq(end), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(start, end, null, null, 0, 10);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldFilterByTypeAndStatusWhenBothAreProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByTypeAndStatus(eq(PayrollRunType.REGULAR), eq(PayrollRunStatus.DRAFT), any(Pageable.class)))
                    .thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, PayrollRunType.REGULAR, PayrollRunStatus.DRAFT, 0, 10);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldFilterByTypeOnlyWhenOnlyTypeIsProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByType(eq(PayrollRunType.REGULAR), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, PayrollRunType.REGULAR, null, 0, 10);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldFilterByStatusOnlyWhenOnlyStatusIsProvided() {
            Page<PayrollRun> page = new PageImpl<>(List.of(draftRun(UUID.randomUUID())));

            when(repository.getAllByStatus(eq(PayrollRunStatus.DRAFT), any(Pageable.class))).thenReturn(page);
            when(mapper.toDto(any(PayrollRun.class))).thenReturn(PayrollRunDto.builder().build());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, null, PayrollRunStatus.DRAFT, 0, 10);

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldReturnEmptyPageWhenNoRunsMatchFilters() {
            when(repository.getAllByStatus(eq(PayrollRunStatus.PROCESSED), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<PayrollRunDto> result = service.getAllPayrollRuns(null, null, null, PayrollRunStatus.PROCESSED, 0, 10);

            assertTrue(result.isEmpty());
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

            assertEquals(id, result.getId());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(repository.findById(id)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getPayrollRunById(id)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

            assertEquals(1, result.getTotalElements());
        }

        @Test
        void shouldReturnEmptyPageWhenNoItemsExistForRun() {
            UUID runId = UUID.randomUUID();
            when(payrollItemRepository.findAllByPayrollRun_Id(eq(runId), any(Pageable.class)))
                    .thenReturn(Page.empty());

            Page<PayrollItemDto> result = service.getPayrollItems(runId, 0, 10);

            assertTrue(result.isEmpty());
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

            assertEquals(itemId, result.getId());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForRun() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.getPayrollItem(runId, itemId)
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

            LineItemEntry entry = new LineItemEntry();
            entry.setCode("SSS");
            entry.setAmount(BigDecimal.valueOf(800));

            UpdatePayrollDeductionRequest request = new UpdatePayrollDeductionRequest();
            request.setDeductions(List.of(entry));

            PayrollItemDto dto = PayrollItemDto.builder().build();

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(dto);

            service.updatePayrollDeductions(runId, itemId, request);

            assertEquals(BigDecimal.valueOf(800), item.getDeductions().getFirst().getAmount());
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

            LineItemEntry entry = new LineItemEntry();
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

            assertEquals(1, item.getDeductions().size());
            assertEquals("PAGIBIG", item.getDeductions().getFirst().getDeduction().getCode());
        }

        @Test
        void shouldRecalculateTotalDeductionsAndNetPayAfterUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            PayrollItem item = itemWithDeductions("SSS", BigDecimal.valueOf(500));
            item.setGrossPay(BigDecimal.valueOf(20000));
            item.setTotalBenefits(BigDecimal.ZERO);

            LineItemEntry entry = new LineItemEntry();
            entry.setCode("SSS");
            entry.setAmount(BigDecimal.valueOf(1000));

            UpdatePayrollDeductionRequest request = new UpdatePayrollDeductionRequest();
            request.setDeductions(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollDeductions(runId, itemId, request);

            assertEquals(BigDecimal.valueOf(1000), item.getTotalDeductions());
            assertEquals(BigDecimal.valueOf(19000), item.getNetPay());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExistForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollDeductions(runId, UUID.randomUUID(), new UpdatePayrollDeductionRequest())
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollDeductions(runId, itemId, new UpdatePayrollDeductionRequest())
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotDraftForDeductionUpdate() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.APPROVED);

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollDeductions(runId, UUID.randomUUID(), new UpdatePayrollDeductionRequest())
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
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

            LineItemEntry entry = new LineItemEntry();
            entry.setCode("RICE");
            entry.setAmount(BigDecimal.valueOf(1000));

            UpdatePayrollBenefitRequest request = new UpdatePayrollBenefitRequest();
            request.setBenefits(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollBenefits(runId, itemId, request);

            assertEquals(BigDecimal.valueOf(1000), item.getBenefits().get(0).getAmount());
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

            LineItemEntry entry = new LineItemEntry();
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

            assertEquals(1, item.getBenefits().size());
            assertEquals("TRANSPORT", item.getBenefits().get(0).getBenefit().getCode());
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

            LineItemEntry entry = new LineItemEntry();
            entry.setCode("RICE");
            entry.setAmount(BigDecimal.valueOf(1500));

            UpdatePayrollBenefitRequest request = new UpdatePayrollBenefitRequest();
            request.setBenefits(List.of(entry));

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.of(item));
            when(payrollItemRepository.save(item)).thenReturn(item);
            when(payrollItemMapper.toDto(item)).thenReturn(PayrollItemDto.builder().build());

            service.updatePayrollBenefits(runId, itemId, request);

            assertEquals(BigDecimal.valueOf(1500), item.getTotalBenefits());
            assertEquals(BigDecimal.valueOf(21500), item.getNetPay());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollRunDoesNotExistForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            when(repository.findById(runId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollBenefits(runId, UUID.randomUUID(), new UpdatePayrollBenefitRequest())
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollItemDoesNotExistForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            UUID itemId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);

            when(repository.findById(runId)).thenReturn(Optional.of(run));
            when(payrollItemRepository.findByPayrollRun_IdAndId(runId, itemId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollBenefits(runId, itemId, new UpdatePayrollBenefitRequest())
            );

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenPayrollRunIsNotDraftForBenefitUpdate() {
            UUID runId = UUID.randomUUID();
            PayrollRun run = draftRun(runId);
            run.setStatus(PayrollRunStatus.PROCESSED);

            when(repository.findById(runId)).thenReturn(Optional.of(run));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> service.updatePayrollBenefits(runId, UUID.randomUUID(), new UpdatePayrollBenefitRequest())
            );

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }
    }

}

