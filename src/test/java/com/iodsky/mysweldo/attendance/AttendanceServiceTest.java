package com.iodsky.mysweldo.attendance;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService service;

    @Mock
    private AttendanceRepository repository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserService userService;

    @Mock
    private AttendanceMapper attendanceMapper;

    private User hrUser;
    private User regularUser;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Role hrRole = new Role("HR");
        Role employeeRole = new Role("EMPLOYEE");

        employee = Employee.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .startShift(LocalTime.of(9, 0))
                .endShift(LocalTime.of(18, 0))
                .build();

        hrUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(hrRole)
                .build();

        Employee otherEmployee = Employee.builder()
                .id(2L)
                .startShift(LocalTime.of(9, 0))
                .endShift(LocalTime.of(18, 0))
                .build();
        regularUser = User.builder()
                .id(UUID.randomUUID())
                .employee(otherEmployee)
                .role(employeeRole)
                .build();
    }

    @Nested
    class CreateAttendanceTests {

        @Test
        void shouldCreateAttendanceWithProvidedTimeInAndOut() {
            LocalDate targetDate = LocalDate.of(2025, 6, 10);
            LocalTime timeIn = LocalTime.of(8, 30);
            LocalTime timeOut = LocalTime.of(17, 30);
            AttendanceRequest request = new AttendanceRequest();
            request.setEmployeeId(1L);
            request.setDate(targetDate);
            request.setTimeIn(timeIn);
            request.setTimeOut(timeOut);

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .date(targetDate)
                    .timeIn(timeIn)
                    .timeOut(timeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .overtime(BigDecimal.ZERO)
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(1L)
                    .date(targetDate)
                    .timeIn(timeIn)
                    .timeOut(timeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .overtimeHours(BigDecimal.ZERO)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findByEmployee_IdAndDate(1L, targetDate)).thenReturn(Optional.empty());
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.createAttendance(request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getDate()).isEqualTo(targetDate);
            assertThat(result.getTimeIn()).isEqualTo(timeIn);
            assertThat(result.getTimeOut()).isEqualTo(timeOut);
        }

        @Test
        void shouldCalculateTotalHoursAndOvertimeWhenCreatingAttendance() {
            LocalDate targetDate = LocalDate.of(2025, 6, 10);
            AttendanceRequest request = new AttendanceRequest();
            request.setEmployeeId(1L);
            request.setDate(targetDate);
            request.setTimeIn(LocalTime.of(9, 0));
            request.setTimeOut(LocalTime.of(20, 0)); // 11 hours with 2 hours overtime

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .date(targetDate)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.of(20, 0))
                    .totalHours(new BigDecimal("11.00"))
                    .overtime(new BigDecimal("2.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(1L)
                    .date(targetDate)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.of(20, 0))
                    .totalHours(new BigDecimal("11.00"))
                    .overtimeHours(new BigDecimal("2.00"))
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findByEmployee_IdAndDate(1L, targetDate)).thenReturn(Optional.empty());
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.createAttendance(request);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("11.00"));
            assertThat(result.getOvertimeHours()).isEqualByComparingTo(new BigDecimal("2.00"));
        }

        @Test
        void shouldThrow409WhenAttendanceRecordAlreadyExistsForDate() {
            LocalDate targetDate = LocalDate.of(2025, 6, 10);
            AttendanceRequest request = new AttendanceRequest();
            request.setEmployeeId(1L);
            request.setDate(targetDate);

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findByEmployee_IdAndDate(1L, targetDate)).thenReturn(Optional.of(Attendance.builder().build()));

            assertThatThrownBy(() -> service.createAttendance(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrow400WhenAttendanceDurationIsTooShort() {
            LocalDate targetDate = LocalDate.of(2025, 6, 10);
            AttendanceRequest request = new AttendanceRequest();
            request.setEmployeeId(1L);
            request.setDate(targetDate);
            request.setTimeIn(LocalTime.of(9, 0));
            request.setTimeOut(LocalTime.of(9, 2)); // Only 2 minutes

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findByEmployee_IdAndDate(1L, targetDate)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createAttendance(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class GetEmployeeAttendanceByDateTests {

        @Test
        void shouldReturnAttendanceWhenRecordExists() {
            LocalDate date = LocalDate.of(2025, 6, 10);
            Attendance attendance = Attendance.builder().date(date).build();
            when(repository.findByEmployee_IdAndDate(1L, date)).thenReturn(Optional.of(attendance));

            Attendance result = service.getEmployeeAttendanceByDate(1L, date);

            assertThat(result).isEqualTo(attendance);
        }

        @Test
        void shouldReturnNullWhenNoRecordExists() {
            LocalDate date = LocalDate.of(2025, 6, 10);
            when(repository.findByEmployee_IdAndDate(1L, date)).thenReturn(Optional.empty());

            Attendance result = service.getEmployeeAttendanceByDate(1L, date);

            assertThat(result).isNull();
        }
    }

    @Nested
    class UpdateAttendanceTests {

        private UUID attendanceId;
        private Attendance attendance;

        @BeforeEach
        void setUp() {
            attendanceId = UUID.randomUUID();
            attendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .date(LocalDate.now())
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.MIN)
                    .build();
        }

        @Test
        void shouldUpdateAttendanceWithNewTimeInAndOut() {
            LocalTime newTimeIn = LocalTime.of(8, 0);
            LocalTime newTimeOut = LocalTime.of(17, 0);
            LocalDate newDate = LocalDate.of(2025, 6, 5);
            AttendanceRequest request = new AttendanceRequest();
            request.setDate(newDate);
            request.setTimeIn(newTimeIn);
            request.setTimeOut(newTimeOut);

            Attendance updatedAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .date(newDate)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .overtime(BigDecimal.ZERO)
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .id(attendanceId)
                    .employeeId(1L)
                    .date(newDate)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .overtimeHours(BigDecimal.ZERO)
                    .build();

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenReturn(updatedAttendance);
            when(attendanceMapper.toDto(updatedAttendance)).thenReturn(expectedDto);

            AttendanceDto result = service.updateAttendance(attendanceId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getTimeIn()).isEqualTo(newTimeIn);
            assertThat(result.getTimeOut()).isEqualTo(newTimeOut);
            assertThat(result.getDate()).isEqualTo(newDate);
        }

        @Test
        void shouldCalculateTotalHoursAndOvertimeAfterUpdate() {
            LocalTime newTimeIn = LocalTime.of(9, 0);
            LocalTime newTimeOut = LocalTime.of(20, 0);
            AttendanceRequest request = new AttendanceRequest();
            request.setDate(LocalDate.now());
            request.setTimeIn(newTimeIn);
            request.setTimeOut(newTimeOut);

            Attendance updatedAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .date(LocalDate.now())
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("11.00"))
                    .overtime(new BigDecimal("2.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .id(attendanceId)
                    .employeeId(1L)
                    .date(LocalDate.now())
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("11.00"))
                    .overtimeHours(new BigDecimal("2.00"))
                    .build();

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenReturn(updatedAttendance);
            when(attendanceMapper.toDto(updatedAttendance)).thenReturn(expectedDto);

            AttendanceDto result = service.updateAttendance(attendanceId, request);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("11.00"));
            assertThat(result.getOvertimeHours()).isEqualByComparingTo(new BigDecimal("2.00"));
        }

        @Test
        void shouldSetOvertimeToZeroWhenWorkedHoursDoNotExceedRegularShift() {
            LocalTime newTimeIn = LocalTime.of(9, 0);
            LocalTime newTimeOut = LocalTime.of(16, 0);
            AttendanceRequest request = new AttendanceRequest();
            request.setDate(LocalDate.now());
            request.setTimeIn(newTimeIn);
            request.setTimeOut(newTimeOut);

            Attendance updatedAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .date(LocalDate.now())
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("7.00"))
                    .overtime(BigDecimal.ZERO)
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .id(attendanceId)
                    .employeeId(1L)
                    .date(LocalDate.now())
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("7.00"))
                    .overtimeHours(BigDecimal.ZERO)
                    .build();

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenReturn(updatedAttendance);
            when(attendanceMapper.toDto(updatedAttendance)).thenReturn(expectedDto);

            AttendanceDto result = service.updateAttendance(attendanceId, request);

            assertThat(result.getOvertimeHours()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void shouldThrow404WhenAttendanceNotFound() {
            AttendanceRequest request = new AttendanceRequest();
            when(repository.findById(attendanceId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrow400WhenAttendanceDurationIsTooShort() {
            LocalTime newTimeIn = LocalTime.of(9, 0);
            LocalTime newTimeOut = LocalTime.of(9, 2); // Only 2 minutes
            AttendanceRequest request = new AttendanceRequest();
            request.setDate(LocalDate.now());
            request.setTimeIn(newTimeIn);
            request.setTimeOut(newTimeOut);

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));

            assertThatThrownBy(() -> service.updateAttendance(attendanceId, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class GetAllAttendancesTests {

        @Test
        void shouldReturnPagedAttendanceDtosWithDescendingCreatedAtSort() {
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(repository.findAllByDateBetween(any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, LocalDate.now().withDayOfMonth(1), LocalDate.now());

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByDateBetween(any(LocalDate.class), any(LocalDate.class), any(Pageable.class));
        }

        @Test
        void shouldReturnAllWhenNoDatesAreProvided() {
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of());

            when(repository.findAllBy(any(Pageable.class)))
                    .thenReturn(attendancePage);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, null, null);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        void shouldReturnFilteredAttendancesWhenOnlyStartDateIsProvided() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(repository.findAllByDateBetween(eq(startDate), eq(LocalDate.now()), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, startDate, null);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByDateBetween(eq(startDate), eq(LocalDate.now()), any(Pageable.class));
        }

        @Test
        void shouldReturnFilteredAttendancesWhenOnlyEndDateIsProvided() {
            LocalDate endDate = LocalDate.of(2026, 4, 3);
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(repository.findAllByDateBetween(any(LocalDate.class), eq(endDate), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, null, endDate);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByDateBetween(any(LocalDate.class), eq(endDate), any(Pageable.class));
        }
    }

    @Nested
    class GetEmployeeAttendancesPaginatedTests {

        @Test
        void shouldReturnOwnAttendancesWhenEmployeeIdIsNull() {
            AttendanceView attendance = AttendanceViewStub.builder()
                    .Employee_FirstName(employee.getFirstName())
                    .Employee_LastName(employee.getLastName())
                    .build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findAllByEmployee_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, null, null, null);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().getEmployeeFirstName()).isEqualTo(dto.getEmployeeFirstName());
        }

        @Test
        void shouldAllowHrToReadAnotherEmployeesAttendances() {
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findAllByEmployee_Id(eq(5L), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 5L, null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldAllowPayrollRoleToReadAnotherEmployeesAttendances() {
            Role payrollRole = new Role("PAYROLL");
            User payrollUser = User.builder()
                    .employee(employee)
                    .role(payrollRole)
                    .build();

            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(5L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(repository.findAllByEmployee_Id(eq(5L), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 5L, null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldAllowEmployeeToReadTheirOwnAttendances() {
            Employee otherEmployee = Employee.builder().id(2L).build();
            User employeeUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(otherEmployee)
                    .role(new Role("EMPLOYEE"))
                    .build();

            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findAllByEmployee_Id(eq(2L), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 2L, null, null);

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        void shouldThrow403WhenNonAdminUserAccessesAnotherEmployeesAttendances() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.getEmployeeAttendances(0, 10, 99L, null, null))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldReturnFilteredAttendancesWhenBothStartAndEndDatesAreProvided() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            LocalDate endDate = LocalDate.of(2026, 4, 20);
            AttendanceView attendance = AttendanceViewStub.builder()
                    .Employee_FirstName(employee.getFirstName())
                    .build();
            AttendanceDto dto = AttendanceDto.builder().employeeFirstName(employee.getFirstName()).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndDateBetween(eq(1L), eq(startDate), eq(endDate), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, startDate, endDate);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(dto);
            verify(repository).findByEmployee_IdAndDateBetween(eq(1L), eq(startDate), eq(endDate), any(Pageable.class));
        }

        @Test
        void shouldReturnFilteredAttendancesWhenOnlyStartDateIsProvided() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            // When only startDate is provided, endDate defaults to today (2026-04-20)
            when(repository.findByEmployee_IdAndDateBetween(eq(1L), eq(startDate), eq(LocalDate.now()), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, startDate, null);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByEmployee_IdAndDateBetween(eq(1L), eq(startDate), eq(LocalDate.now()), any(Pageable.class));
        }

        @Test
        void shouldReturnFilteredAttendancesWhenOnlyEndDateIsProvided() {
            LocalDate endDate = LocalDate.of(2026, 4, 3);
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            // When only endDate is provided, startDate defaults to 1900-01-01
            when(repository.findByEmployee_IdAndDateBetween(eq(1L), any(LocalDate.class), eq(endDate), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, null, endDate);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByEmployee_IdAndDateBetween(eq(1L), any(LocalDate.class), eq(endDate), any(Pageable.class));
        }

        @Test
        void shouldReturnAllAttendancesWhenNoDatesAreProvided() {
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findAllByEmployee_Id(eq(1L), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, null, null);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByEmployee_Id(eq(1L), any(Pageable.class));
        }

    }

    @Nested
    class ClockInAuthenticatedEmployeeTests {

        @Test
        void shouldClockInAuthenticatedEmployeeSuccessfully() {
            Attendance attendance = Attendance.builder()
                    .employee(regularUser.getEmployee())
                    .date(LocalDate.now())
                    .timeIn(LocalTime.now())
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(2L)
                    .date(LocalDate.now())
                    .timeIn(attendance.getTimeIn())
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndTimeOutIsNull(2L)).thenReturn(false);
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.clockIn();

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getEmployeeId()).isEqualTo(2L);
            assertThat(result.getDate()).isEqualTo(LocalDate.now());
            verify(repository).save(any(Attendance.class));
        }

        @Test
        void shouldThrow409WhenEmployeeAlreadyHasOpenAttendance() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndTimeOutIsNull(2L)).thenReturn(true);

            assertThatThrownBy(() -> service.clockIn())
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

    }

    @Nested
    class ClockOutAuthenticatedEmployeeTests {

        private Attendance openAttendance;

        @BeforeEach
        void setUp() {
            openAttendance = Attendance.builder()
                    .employee(regularUser.getEmployee())
                    .date(LocalDate.now())
                    .timeIn(LocalTime.of(9, 0))
                    .build();
        }

        @Test
        void shouldClockOutAuthenticatedEmployeeSuccessfully() {
            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(2L)
                    .date(LocalDate.now())
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.of(18, 0))
                    .totalHours(new BigDecimal("9.00"))
                    .overtimeHours(BigDecimal.ZERO)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByDateDescTimeInDesc(2L))
                    .thenReturn(Optional.of(openAttendance));
            when(repository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(attendanceMapper.toDto(any(Attendance.class))).thenReturn(expectedDto);

            AttendanceDto result = service.clockOut();

            assertThat(result).isNotNull();
            verify(repository).save(any(Attendance.class));
            verify(attendanceMapper).toDto(any(Attendance.class));
        }

        @Test
        void shouldThrow400WhenNoOpenAttendanceFound() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByDateDescTimeInDesc(2L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.clockOut())
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class GetAttendanceSummaryTests {

        private static final LocalDate MON = LocalDate.of(2026, 3, 9);
        private static final LocalDate FRI = LocalDate.of(2026, 3, 13);

        private Employee shiftEmployee;

        @BeforeEach
        void setUp() {
            shiftEmployee = Employee.builder()
                    .id(1L)
                    .startShift(LocalTime.of(9, 0))
                    .endShift(LocalTime.of(18, 0))
                    .build();
        }

        @Test
        void shouldReturnZeroTardinessWhenEmployeeArrivesBeforeThreshold() {
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .date(MON)
                    .timeIn(LocalTime.of(9, 10))
                    .timeOut(LocalTime.of(18, 0))
                    .build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, MON)).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isZero();
        }

        @Test
        void shouldCalculateTardinessFromShiftStartWhenEmployeeIsLate() {
            // 9:30 is after the 9:15 tardiness threshold; tardiness is measured from shift start (9:00)
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .date(MON)
                    .timeIn(LocalTime.of(9, 30))
                    .timeOut(LocalTime.of(18, 0))
                    .build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, MON)).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isEqualTo(30);
        }

        @Test
        void shouldReturnZeroUndertimeWhenEmployeeLeavesDuringOrAfterThreshold() {
            // 17:50 is after the 17:45 undertime threshold
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .date(MON)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.of(17, 50))
                    .build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, MON)).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getUndertimeMinutes()).isZero();
        }

        @Test
        void shouldCalculateUndertimeFromTimeOutToShiftEndWhenEmployeeLeavesEarly() {
            // 17:00 is before the 17:45 undertime threshold; undertime is from timeOut to shift end
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .date(MON)
                    .timeIn(LocalTime.of(9, 0))
                    .timeOut(LocalTime.of(17, 0))
                    .build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, MON)).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getUndertimeMinutes()).isEqualTo(60);
        }

        @Test
        void shouldCalculateAbsenceDaysAsExpectedWorkdaysMinusDaysWorked() {
            // Mon–Fri = 5 weekdays; only 3 attendances recorded
            List<Attendance> attendances = List.of(
                    Attendance.builder().employee(shiftEmployee).date(MON).timeIn(LocalTime.of(9, 0)).timeOut(LocalTime.of(18, 0)).build(),
                    Attendance.builder().employee(shiftEmployee).date(MON.plusDays(1)).timeIn(LocalTime.of(9, 0)).timeOut(LocalTime.of(18, 0)).build(),
                    Attendance.builder().employee(shiftEmployee).date(MON.plusDays(2)).timeIn(LocalTime.of(9, 0)).timeOut(LocalTime.of(18, 0)).build()
            );
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, FRI)).thenReturn(attendances);

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, FRI);

            assertThat(result.getDaysWorked()).isEqualByComparingTo(new BigDecimal("3"));
            assertThat(result.getAbsenceDays()).isEqualByComparingTo(new BigDecimal("2"));
        }

        @Test
        void shouldExcludeWeekendsFromExpectedWorkdayCount() {
            // Mon–Fri has 5 weekdays; full attendance means 0 absences
            LocalDate sun = LocalDate.of(2026, 3, 15);
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, sun)).thenReturn(List.of());

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, sun);

            assertThat(result.getAbsenceDays()).isEqualByComparingTo(new BigDecimal("5"));
        }

        @Test
        void shouldAccumulateTardinessAcrossMultipleAttendanceDays() {
            Attendance day1 = Attendance.builder().employee(shiftEmployee).date(MON)
                    .timeIn(LocalTime.of(9, 30)).timeOut(LocalTime.of(18, 0)).build();
            Attendance day2 = Attendance.builder().employee(shiftEmployee).date(MON.plusDays(1))
                    .timeIn(LocalTime.of(9, 20)).timeOut(LocalTime.of(18, 0)).build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, FRI)).thenReturn(List.of(day1, day2));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, FRI);

            assertThat(result.getTardinessMinutes()).isEqualTo(50); // 30 + 20
        }

        @Test
        void shouldNotCalculateTardinessOrUndertimeWhenShiftTimesAreNull() {
            Employee employeeWithNoShift = Employee.builder().id(1L).build();
            Attendance attendance = Attendance.builder()
                    .employee(employeeWithNoShift)
                    .date(MON)
                    .timeIn(LocalTime.of(11, 0))
                    .timeOut(LocalTime.of(14, 0))
                    .build();
            when(repository.findByEmployee_IdAndDateBetween(1L, MON, MON)).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isZero();
            assertThat(result.getUndertimeMinutes()).isZero();
        }
    }
}



