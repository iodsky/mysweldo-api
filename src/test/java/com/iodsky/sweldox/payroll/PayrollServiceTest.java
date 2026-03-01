package com.iodsky.sweldox.payroll;

import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.benefit.Benefit;
import com.iodsky.sweldox.benefit.BenefitType;
import com.iodsky.sweldox.payroll.core.*;
import com.iodsky.sweldox.payroll.deduction.Deduction;
import com.iodsky.sweldox.payroll.deduction.DeductionType;
import com.iodsky.sweldox.security.user.User;
import com.iodsky.sweldox.security.role.Role;
import com.iodsky.sweldox.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayrollRepository payrollRepository;
    @Mock private UserService userService;
    @Mock private PayrollBuilder payrollBuilder;
    @InjectMocks private PayrollService payrollService;

    private User payrollUser;
    private User hrUser;
    private User normalUser;
    private Employee employee;
    private Employee otherEmployee;
    private Payroll payroll;
    private DeductionType sssType;
    private DeductionType phicType;
    private DeductionType hdmfType;
    private DeductionType taxType;
    private BenefitType riceAllowanceType;
    private Benefit riceBenefit;

    private static final LocalDate PERIOD_START = LocalDate.of(2025, 11, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2025, 11, 15);
    private static final LocalDate PAY_DATE = LocalDate.of(2025, 11, 20);
    private static final YearMonth YEAR_MONTH = YearMonth.of(2025, 11);
    private static final BigDecimal BASIC_SALARY = new BigDecimal("30000.00");
    private static final BigDecimal SEMI_MONTHLY_RATE = new BigDecimal("30000.00");
    private static final BigDecimal HOURLY_RATE = new BigDecimal("178.57");

    @BeforeEach
    void setUp() {
        // Setup employees
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Juan");
        employee.setLastName("Dela Cruz");

        otherEmployee = new Employee();
        otherEmployee.setId(2L);
        otherEmployee.setFirstName("Maria");
        otherEmployee.setLastName("Santos");

        // Setup compensation
        riceAllowanceType = BenefitType.builder()
                .id("RICE")
                .type("Rice Allowance")
                .build();

        riceBenefit = Benefit.builder()
                .id(UUID.randomUUID())
                .benefitType(riceAllowanceType)
                .amount(new BigDecimal("2000.00"))
                .build();

        employee.setBasicSalary(BASIC_SALARY);
        employee.setHourlyRate(HOURLY_RATE);
        employee.setSemiMonthlyRate(SEMI_MONTHLY_RATE);
        employee.setBenefits(List.of(riceBenefit));

        // Setup deduction types
        sssType = DeductionType.builder()
                .code("SSS")
                .type("SSS Contribution")
                .build();

        phicType = DeductionType.builder()
                .code("PHIC")
                .type("PhilHealth Contribution")
                .build();

        hdmfType = DeductionType.builder()
                .code("HDMF")
                .type("Pag-IBIG Contribution")
                .build();

        taxType = DeductionType.builder()
                .code("TAX")
                .type("Withholding Tax")
                .build();

        // Setup users
        payrollUser = new User();
        payrollUser.setRole(new Role("PAYROLL"));
        payrollUser.setEmployee(employee);

        hrUser = new User();
        hrUser.setRole(new Role("HR"));
        hrUser.setEmployee(employee);

        normalUser = new User();
        normalUser.setRole(new Role("EMPLOYEE"));
        normalUser.setEmployee(employee);

        // Setup payroll
        payroll = Payroll.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .periodStartDate(PERIOD_START)
                .periodEndDate(PERIOD_END)
                .payDate(PAY_DATE)
                .monthlyRate(BASIC_SALARY)
                .dailyRate(new BigDecimal("1428.57"))
                .daysWorked(10)
                .overtime(BigDecimal.ZERO)
                .grossPay(new BigDecimal("14285.70"))
                .totalBenefits(new BigDecimal("2000.00"))
                .totalDeductions(new BigDecimal("2500.00"))
                .netPay(new BigDecimal("13785.70"))
                .deductions(new ArrayList<>())
                .benefits(new ArrayList<>())
                .build();
    }


    @Nested
    class CreatePayrollTests {

        @Test
        void shouldCreatePayrollSuccessfully() {
            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payroll);
            when(payrollRepository.save(payroll)).thenReturn(payroll);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            assertEquals(employee, result.getEmployee());
            assertEquals(PERIOD_START, result.getPeriodStartDate());
            assertEquals(PERIOD_END, result.getPeriodEndDate());
            assertEquals(PAY_DATE, result.getPayDate());

            verify(payrollBuilder).buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);
            verify(payrollRepository).save(payroll);
        }

        @Test
        void shouldThrowConflictWhenPayrollAlreadyExists() {
            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            verify(payrollRepository, never()).save(any(Payroll.class));
        }

        @Test
        void shouldHandleEmployeeWithNoAttendances() {
            Payroll payrollWithNoAttendance = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .daysWorked(0)
                    .grossPay(BigDecimal.ZERO)
                    .netPay(BigDecimal.ZERO)
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payrollWithNoAttendance);
            when(payrollRepository.save(payrollWithNoAttendance)).thenReturn(payrollWithNoAttendance);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            assertEquals(0, result.getDaysWorked());
            verify(payrollBuilder).buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);
            verify(payrollRepository).save(payrollWithNoAttendance);
        }

        @Test
        void shouldHandleEmployeeWithOvertimeHours() {
            Payroll payrollWithOvertime = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .daysWorked(1)
                    .overtime(new BigDecimal("4.00"))
                    .grossPay(new BigDecimal("2000.00"))
                    .netPay(new BigDecimal("1800.00"))
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payrollWithOvertime);
            when(payrollRepository.save(payrollWithOvertime)).thenReturn(payrollWithOvertime);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            verify(payrollBuilder).buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);
            verify(payrollRepository).save(payrollWithOvertime);
        }

        @Test
        void shouldThrowExceptionWhenPayrollBuilderFails() {
            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenThrow(new NoSuchElementException("Employee not found"));

            assertThrows(NoSuchElementException.class,
                    () -> payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE));

            verify(payrollRepository, never()).save(any(Payroll.class));
        }

        @Test
        void shouldCalculateCorrectDeductions() {
            Deduction sssDeduction = Deduction.builder()
                    .id(UUID.randomUUID())
                    .deductionType(sssType)
                    .amount(new BigDecimal("1125.00"))
                    .build();
            Deduction phicDeduction = Deduction.builder()
                    .id(UUID.randomUUID())
                    .deductionType(phicType)
                    .amount(new BigDecimal("750.00"))
                    .build();
            Deduction hdmfDeduction = Deduction.builder()
                    .id(UUID.randomUUID())
                    .deductionType(hdmfType)
                    .amount(new BigDecimal("100.00"))
                    .build();
            Deduction taxDeduction = Deduction.builder()
                    .id(UUID.randomUUID())
                    .deductionType(taxType)
                    .amount(new BigDecimal("525.00"))
                    .build();

            Payroll payrollWithDeductions = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .deductions(Arrays.asList(sssDeduction, phicDeduction, hdmfDeduction, taxDeduction))
                    .benefits(new ArrayList<>())
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payrollWithDeductions);
            when(payrollRepository.save(payrollWithDeductions)).thenReturn(payrollWithDeductions);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            assertEquals(4, result.getDeductions().size());
            assertTrue(result.getDeductions().stream()
                    .anyMatch(d -> d.getDeductionType().getCode().equals("SSS")));
            assertTrue(result.getDeductions().stream()
                    .anyMatch(d -> d.getDeductionType().getCode().equals("PHIC")));
            assertTrue(result.getDeductions().stream()
                    .anyMatch(d -> d.getDeductionType().getCode().equals("HDMF")));
            assertTrue(result.getDeductions().stream()
                    .anyMatch(d -> d.getDeductionType().getCode().equals("TAX")));
        }

        @Test
        void shouldIncludeBenefitsInPayroll() {
            PayrollBenefit payrollRiceBenefit = PayrollBenefit.builder()
                    .id(UUID.randomUUID())
                    .benefitType(riceAllowanceType)
                    .amount(new BigDecimal("2000.00"))
                    .build();

            Payroll payrollWithBenefits = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .deductions(new ArrayList<>())
                    .benefits(List.of(payrollRiceBenefit))
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payrollWithBenefits);
            when(payrollRepository.save(payrollWithBenefits)).thenReturn(payrollWithBenefits);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            assertNotNull(result.getBenefits());
            assertEquals(1, result.getBenefits().size());
            assertEquals(riceAllowanceType, result.getBenefits().get(0).getBenefitType());
        }
    }

    @Nested
    class GetPayrollByIdTests {

        @Test
        void shouldReturnPayrollForPayrollUser() {
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findById(payroll.getId())).thenReturn(Optional.of(payroll));

            Payroll result = payrollService.getPayrollById(payroll.getId());

            assertNotNull(result);
            assertEquals(payroll.getId(), result.getId());
            verify(payrollRepository).findById(payroll.getId());
        }

        @Test
        void shouldAllowEmployeeToViewOwnPayroll() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            payroll.setEmployee(employee);
            when(payrollRepository.findById(payroll.getId())).thenReturn(Optional.of(payroll));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getPayrollById(payroll.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowUnauthorizedWhenPrincipalIsNotUser() {
            when(userService.getAuthenticatedUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getPayrollById(payroll.getId()));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            verify(payrollRepository, never()).findById(any());
        }

        @Test
        void shouldThrowNotFoundWhenPayrollDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            UUID nonExistentId = UUID.randomUUID();
            when(payrollRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getPayrollById(nonExistentId));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowForbiddenWhenPayrollUserAccessesOtherEmployeePayroll() {
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            payroll.setEmployee(otherEmployee);
            when(payrollRepository.findById(payroll.getId())).thenReturn(Optional.of(payroll));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getPayrollById(payroll.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowForbiddenWhenNonPayrollUserTries() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            payroll.setEmployee(employee);
            when(payrollRepository.findById(payroll.getId())).thenReturn(Optional.of(payroll));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getPayrollById(payroll.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }
    }

    @Nested
    class GetAllPayrollTests {

        @Test
        void shouldReturnAllPayrollWithinPeriod() {
            List<Payroll> payrolls = List.of(payroll);
            Page<Payroll> page = new PageImpl<>(payrolls);

            when(payrollRepository.findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(page);

            Page<Payroll> result = payrollService.getAllPayroll(0, 10, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(payrollRepository).findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoPayrollsFound() {
            Page<Payroll> emptyPage = new PageImpl<>(Collections.emptyList());

            when(payrollRepository.findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<Payroll> result = payrollService.getAllPayroll(0, 10, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }

        @Test
        void shouldHandlePaginationCorrectly() {
            List<Payroll> payrolls = Arrays.asList(payroll, payroll, payroll);
            Page<Payroll> page = new PageImpl<>(payrolls, PageRequest.of(1, 2), 5);

            when(payrollRepository.findAllByPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class))).thenReturn(page);

            Page<Payroll> result = payrollService.getAllPayroll(1, 2, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(3, result.getContent().size());
            assertEquals(5, result.getTotalElements());
            assertEquals(1, result.getNumber());
        }

        @Test
        void shouldReturnAllPayrollsWhenNoPeriodProvided() {
            List<Payroll> payrolls = Arrays.asList(payroll, payroll);
            Page<Payroll> page = new PageImpl<>(payrolls);

            when(payrollRepository.findAll(any(Pageable.class))).thenReturn(page);

            Page<Payroll> result = payrollService.getAllPayroll(0, 10, null);

            assertNotNull(result);
            assertEquals(2, result.getTotalElements());
            verify(payrollRepository).findAll(any(Pageable.class));
        }
    }

    @Nested
    class GetAllEmployeePayrollTests {

        @Test
        void shouldReturnEmployeePayrolls() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            List<Payroll> payrolls = List.of(payroll);
            Page<Payroll> page = new PageImpl<>(payrolls);

            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(page);

            Page<Payroll> result = payrollService.getAllEmployeePayroll(0, 10, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(payrollRepository).findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class));
        }

        @Test
        void shouldThrowUnauthorizedWhenPrincipalIsNotUser() {
            when(userService.getAuthenticatedUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> payrollService.getAllEmployeePayroll(0, 10, YEAR_MONTH));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());

            verify(payrollRepository, never()).findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    anyLong(), any(), any(), any());
        }

        @Test
        void shouldReturnEmptyPageWhenEmployeeHasNoPayrolls() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Page<Payroll> emptyPage = new PageImpl<>(Collections.emptyList());

            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<Payroll> result = payrollService.getAllEmployeePayroll(0, 10, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
        }

        @Test
        void shouldOnlyReturnPayrollsForAuthenticatedEmployee() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Page<Payroll> page = new PageImpl<>(List.of(payroll));

            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(page);

            payrollService.getAllEmployeePayroll(0, 10, YEAR_MONTH);

            verify(payrollRepository).findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class));
            verify(payrollRepository, never()).findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(otherEmployee.getId()), any(), any(), any());
        }

        @Test
        void shouldHandlePaginationForEmployeePayrolls() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            List<Payroll> payrolls = Arrays.asList(payroll, payroll);
            Page<Payroll> page = new PageImpl<>(payrolls, PageRequest.of(0, 2), 10);

            when(payrollRepository.findAllByEmployee_IdAndPeriodStartDateLessThanEqualAndPeriodEndDateGreaterThanEqual(
                    eq(employee.getId()), eq(YEAR_MONTH.atEndOfMonth()), eq(YEAR_MONTH.atDay(1)), any(Pageable.class)))
                    .thenReturn(page);

            Page<Payroll> result = payrollService.getAllEmployeePayroll(0, 2, YEAR_MONTH);

            assertNotNull(result);
            assertEquals(2, result.getContent().size());
            assertEquals(10, result.getTotalElements());
        }

        @Test
        void shouldReturnAllEmployeePayrollsWhenNoPeriodProvided() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Page<Payroll> page = new PageImpl<>(Collections.emptyList());

            when(payrollRepository.findAllByEmployee_Id(eq(employee.getId()), any(Pageable.class)))
                    .thenReturn(page);

            payrollService.getAllEmployeePayroll(0, 10, null);

            verify(payrollRepository).findAllByEmployee_Id(eq(employee.getId()), any(Pageable.class));
        }
    }

    @Nested
    class EdgeCaseTests {

        @Test
        void shouldHandleNullBenefitsList() {
            employee.setBenefits(Collections.emptyList());

            Payroll payrollWithoutBenefits = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .deductions(new ArrayList<>())
                    .benefits(Collections.emptyList())
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(payrollWithoutBenefits);
            when(payrollRepository.save(payrollWithoutBenefits)).thenReturn(payrollWithoutBenefits);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            assertTrue(result.getBenefits().isEmpty());
        }

        @Test
        void shouldHandleVeryLowSalaryEmployee() {
            employee.setBasicSalary(new BigDecimal("5000.00"));
            employee.setHourlyRate(new BigDecimal("29.76"));

            Payroll lowSalaryPayroll = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .daysWorked(5)
                    .grossPay(new BigDecimal("1000.00"))
                    .netPay(new BigDecimal("900.00"))
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(lowSalaryPayroll);
            when(payrollRepository.save(lowSalaryPayroll)).thenReturn(lowSalaryPayroll);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            verify(payrollRepository).save(lowSalaryPayroll);
        }

        @Test
        void shouldHandleMaxOvertimeHours() {
            Payroll overtimePayroll = Payroll.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .periodStartDate(PERIOD_START)
                    .periodEndDate(PERIOD_END)
                    .payDate(PAY_DATE)
                    .daysWorked(15)
                    .overtime(new BigDecimal("90.00"))
                    .grossPay(new BigDecimal("25000.00"))
                    .netPay(new BigDecimal("22000.00"))
                    .build();

            when(payrollRepository.existsByEmployee_IdAndPeriodStartDateAndPeriodEndDate(
                    employee.getId(), PERIOD_START, PERIOD_END)).thenReturn(false);
            when(payrollBuilder.buildPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE))
                    .thenReturn(overtimePayroll);
            when(payrollRepository.save(overtimePayroll)).thenReturn(overtimePayroll);

            Payroll result = payrollService.createPayroll(employee.getId(), PERIOD_START, PERIOD_END, PAY_DATE);

            assertNotNull(result);
            verify(payrollRepository).save(overtimePayroll);
        }
    }
}

