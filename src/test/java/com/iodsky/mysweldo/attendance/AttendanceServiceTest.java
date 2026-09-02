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
import java.time.LocalDateTime;
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

        private AttendanceRequest request(LocalDateTime timeIn, LocalDateTime timeOut) {
            AttendanceRequest request = new AttendanceRequest();
            request.setEmployeeId(1L);
            request.setTimeIn(timeIn);
            request.setTimeOut(timeOut);
            return request;
        }

        @Test
        void shouldCreateAttendanceWithProvidedTimeInAndOut() {
            LocalDateTime timeIn = LocalDateTime.of(2025, 6, 10, 8, 30);
            LocalDateTime timeOut = LocalDateTime.of(2025, 6, 10, 17, 30);
            AttendanceRequest request = request(timeIn, timeOut);

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .timeIn(timeIn)
                    .timeOut(timeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(1L)
                    .timeIn(timeIn)
                    .timeOut(timeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findFirstByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(Optional.empty());
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.createAttendance(request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getTimeIn()).isEqualTo(timeIn);
            assertThat(result.getTimeOut()).isEqualTo(timeOut);
        }

        @Test
        void shouldCalculateTotalHoursWhenCreatingAttendance() {
            AttendanceRequest request = request(
                    LocalDateTime.of(2025, 6, 10, 9, 0),
                    LocalDateTime.of(2025, 6, 10, 20, 0) // 11 hours
            );

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .timeIn(request.getTimeIn())
                    .timeOut(request.getTimeOut())
                    .totalHours(new BigDecimal("11.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(1L)
                    .timeIn(request.getTimeIn())
                    .timeOut(request.getTimeOut())
                    .totalHours(new BigDecimal("11.00"))
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findFirstByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(Optional.empty());
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.createAttendance(request);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("11.00"));
        }

        @Test
        void shouldHandleOvernightShiftAcrossMidnight() {
            AttendanceRequest request = request(
                    LocalDateTime.of(2025, 6, 10, 22, 0),
                    LocalDateTime.of(2025, 6, 11, 6, 0) // 8 hours across midnight
            );

            Attendance attendance = Attendance.builder()
                    .employee(employee)
                    .timeIn(request.getTimeIn())
                    .timeOut(request.getTimeOut())
                    .totalHours(new BigDecimal("8.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(1L)
                    .timeIn(request.getTimeIn())
                    .timeOut(request.getTimeOut())
                    .totalHours(new BigDecimal("8.00"))
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findFirstByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(Optional.empty());
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.createAttendance(request);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("8.00"));
        }

        @Test
        void shouldThrow409WhenAttendanceRecordAlreadyExistsForDate() {
            LocalDateTime timeIn = LocalDateTime.of(2025, 6, 10, 9, 0);
            AttendanceRequest request = request(timeIn, LocalDateTime.of(2025, 6, 10, 18, 0));

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.findFirstByEmployee_IdAndTimeInBetween(
                    eq(1L), eq(timeIn.toLocalDate().atStartOfDay()), eq(timeIn.toLocalDate().plusDays(1).atStartOfDay())))
                    .thenReturn(Optional.of(Attendance.builder().build()));

            assertThatThrownBy(() -> service.createAttendance(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrow400WhenAttendanceDurationIsTooShort() {
            AttendanceRequest request = request(
                    LocalDateTime.of(2025, 6, 10, 9, 0),
                    LocalDateTime.of(2025, 6, 10, 9, 2) // Only 2 minutes
            );

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);

            assertThatThrownBy(() -> service.createAttendance(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenTimeOutIsBeforeTimeIn() {
            AttendanceRequest request = request(
                    LocalDateTime.of(2025, 6, 10, 18, 0),
                    LocalDateTime.of(2025, 6, 10, 9, 0)
            );

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);

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
            Attendance attendance = Attendance.builder().timeIn(date.atStartOfDay()).build();
            when(repository.findFirstByEmployee_IdAndTimeInBetween(
                    eq(1L), eq(date.atStartOfDay()), eq(date.plusDays(1).atStartOfDay())))
                    .thenReturn(Optional.of(attendance));

            Attendance result = service.getEmployeeAttendanceByDate(1L, date);

            assertThat(result).isEqualTo(attendance);
        }

        @Test
        void shouldReturnNullWhenNoRecordExists() {
            LocalDate date = LocalDate.of(2025, 6, 10);
            when(repository.findFirstByEmployee_IdAndTimeInBetween(
                    eq(1L), eq(date.atStartOfDay()), eq(date.plusDays(1).atStartOfDay())))
                    .thenReturn(Optional.empty());

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
                    .timeIn(LocalDateTime.of(2025, 6, 10, 9, 0))
                    .timeOut(null)
                    .build();
        }

        private AttendanceRequest request(LocalDateTime timeIn, LocalDateTime timeOut) {
            AttendanceRequest request = new AttendanceRequest();
            request.setTimeIn(timeIn);
            request.setTimeOut(timeOut);
            return request;
        }

        @Test
        void shouldUpdateAttendanceWithNewTimeInAndOut() {
            LocalDateTime newTimeIn = LocalDateTime.of(2025, 6, 5, 8, 0);
            LocalDateTime newTimeOut = LocalDateTime.of(2025, 6, 5, 17, 0);
            AttendanceRequest request = request(newTimeIn, newTimeOut);

            Attendance updatedAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .id(attendanceId)
                    .employeeId(1L)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("9.00"))
                    .build();

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenReturn(updatedAttendance);
            when(attendanceMapper.toDto(updatedAttendance)).thenReturn(expectedDto);

            AttendanceDto result = service.updateAttendance(attendanceId, request);

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getTimeIn()).isEqualTo(newTimeIn);
            assertThat(result.getTimeOut()).isEqualTo(newTimeOut);
        }

        @Test
        void shouldCalculateTotalHoursAfterUpdate() {
            LocalDateTime newTimeIn = LocalDateTime.of(2025, 6, 10, 9, 0);
            LocalDateTime newTimeOut = LocalDateTime.of(2025, 6, 10, 20, 0);
            AttendanceRequest request = request(newTimeIn, newTimeOut);

            Attendance updatedAttendance = Attendance.builder()
                    .id(attendanceId)
                    .employee(employee)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("11.00"))
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .id(attendanceId)
                    .employeeId(1L)
                    .timeIn(newTimeIn)
                    .timeOut(newTimeOut)
                    .totalHours(new BigDecimal("11.00"))
                    .build();

            when(repository.findById(attendanceId)).thenReturn(Optional.of(attendance));
            when(repository.save(any(Attendance.class))).thenReturn(updatedAttendance);
            when(attendanceMapper.toDto(updatedAttendance)).thenReturn(expectedDto);

            AttendanceDto result = service.updateAttendance(attendanceId, request);

            assertThat(result.getTotalHours()).isEqualByComparingTo(new BigDecimal("11.00"));
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
            AttendanceRequest request = request(
                    LocalDateTime.of(2025, 6, 10, 9, 0),
                    LocalDateTime.of(2025, 6, 10, 9, 2) // Only 2 minutes
            );

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

            when(repository.findAllByTimeInBetween(any(), any(), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, LocalDate.now().withDayOfMonth(1), LocalDate.now());

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByTimeInBetween(any(), any(), any(Pageable.class));
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

            when(repository.findAllByTimeInBetween(any(), any(), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getAllAttendances(0, 10, startDate, null);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findAllByTimeInBetween(any(), any(), any(Pageable.class));
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
            when(repository.findByEmployee_IdAndTimeInBetween(
                    eq(1L), eq(startDate.atStartOfDay()), eq(endDate.plusDays(1).atStartOfDay()), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, startDate, endDate);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst()).isEqualTo(dto);
            verify(repository).findByEmployee_IdAndTimeInBetween(
                    eq(1L), eq(startDate.atStartOfDay()), eq(endDate.plusDays(1).atStartOfDay()), any(Pageable.class));
        }

        @Test
        void shouldReturnFilteredAttendancesWhenOnlyStartDateIsProvided() {
            LocalDate startDate = LocalDate.of(2026, 1, 1);
            AttendanceView attendance = AttendanceViewStub.builder().build();
            AttendanceDto dto = AttendanceDto.builder().employeeId(1L).build();
            Page<AttendanceView> attendancePage = new PageImpl<>(List.of(attendance));

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_IdAndTimeInBetween(eq(1L), any(), any(), any(Pageable.class)))
                    .thenReturn(attendancePage);
            when(attendanceMapper.toDto(attendance)).thenReturn(dto);

            Page<AttendanceDto> result = service.getEmployeeAttendances(0, 10, 1L, startDate, null);

            assertThat(result.getContent()).hasSize(1);
            verify(repository).findByEmployee_IdAndTimeInBetween(eq(1L), any(), any(), any(Pageable.class));
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
                    .timeIn(LocalDateTime.now())
                    .build();

            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(2L)
                    .timeIn(attendance.getTimeIn())
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndTimeOutIsNull(2L)).thenReturn(false);
            when(repository.save(any(Attendance.class))).thenReturn(attendance);
            when(attendanceMapper.toDto(attendance)).thenReturn(expectedDto);

            AttendanceDto result = service.clockIn();

            assertThat(result).isEqualTo(expectedDto);
            assertThat(result.getEmployeeId()).isEqualTo(2L);
            assertThat(result.getTimeIn()).isNotNull();
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
                    .timeIn(LocalDateTime.now().minusMinutes(30))
                    .build();
        }

        @Test
        void shouldClockOutAuthenticatedEmployeeSuccessfully() {
            AttendanceDto expectedDto = AttendanceDto.builder()
                    .employeeId(2L)
                    .timeIn(openAttendance.getTimeIn())
                    .timeOut(LocalDateTime.now())
                    .totalHours(new BigDecimal("0.50"))
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByTimeInDesc(2L))
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
            when(repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByTimeInDesc(2L))
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
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(9, 10)))
                    .timeOut(LocalDateTime.of(MON, LocalTime.of(18, 0)))
                    .build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isZero();
        }

        @Test
        void shouldCalculateTardinessFromShiftStartWhenEmployeeIsLate() {
            // 9:30 is after the 9:15 tardiness threshold; tardiness is measured from shift start (9:00)
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(9, 30)))
                    .timeOut(LocalDateTime.of(MON, LocalTime.of(18, 0)))
                    .build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isEqualTo(30);
        }

        @Test
        void shouldReturnZeroUndertimeWhenEmployeeLeavesDuringOrAfterThreshold() {
            // 17:50 is after the 17:45 undertime threshold
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(9, 0)))
                    .timeOut(LocalDateTime.of(MON, LocalTime.of(17, 50)))
                    .build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getUndertimeMinutes()).isZero();
        }

        @Test
        void shouldCalculateUndertimeFromTimeOutToShiftEndWhenEmployeeLeavesEarly() {
            // 17:00 is before the 17:45 undertime threshold; undertime is from timeOut to shift end
            Attendance attendance = Attendance.builder()
                    .employee(shiftEmployee)
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(9, 0)))
                    .timeOut(LocalDateTime.of(MON, LocalTime.of(17, 0)))
                    .build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getUndertimeMinutes()).isEqualTo(60);
        }

        @Test
        void shouldCalculateAbsenceDaysAsExpectedWorkdaysMinusDaysWorked() {
            // Mon–Fri = 5 weekdays; only 3 attendances recorded
            List<Attendance> attendances = List.of(
                    Attendance.builder().employee(shiftEmployee).timeIn(LocalDateTime.of(MON, LocalTime.of(9, 0))).timeOut(LocalDateTime.of(MON, LocalTime.of(18, 0))).build(),
                    Attendance.builder().employee(shiftEmployee).timeIn(LocalDateTime.of(MON.plusDays(1), LocalTime.of(9, 0))).timeOut(LocalDateTime.of(MON.plusDays(1), LocalTime.of(18, 0))).build(),
                    Attendance.builder().employee(shiftEmployee).timeIn(LocalDateTime.of(MON.plusDays(2), LocalTime.of(9, 0))).timeOut(LocalDateTime.of(MON.plusDays(2), LocalTime.of(18, 0))).build()
            );
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(attendances);

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, FRI);

            assertThat(result.getDaysWorked()).isEqualByComparingTo(new BigDecimal("3"));
            assertThat(result.getAbsenceDays()).isEqualByComparingTo(new BigDecimal("2"));
        }

        @Test
        void shouldExcludeWeekendsFromExpectedWorkdayCount() {
            // Mon–Fri has 5 weekdays; full attendance means 0 absences
            LocalDate sun = LocalDate.of(2026, 3, 15);
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of());

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, sun);

            assertThat(result.getAbsenceDays()).isEqualByComparingTo(new BigDecimal("5"));
        }

        @Test
        void shouldAccumulateTardinessAcrossMultipleAttendanceDays() {
            Attendance day1 = Attendance.builder().employee(shiftEmployee)
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(9, 30))).timeOut(LocalDateTime.of(MON, LocalTime.of(18, 0))).build();
            Attendance day2 = Attendance.builder().employee(shiftEmployee)
                    .timeIn(LocalDateTime.of(MON.plusDays(1), LocalTime.of(9, 20))).timeOut(LocalDateTime.of(MON.plusDays(1), LocalTime.of(18, 0))).build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(day1, day2));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, FRI);

            assertThat(result.getTardinessMinutes()).isEqualTo(50); // 30 + 20
        }

        @Test
        void shouldNotCalculateTardinessOrUndertimeWhenShiftTimesAreNull() {
            Employee employeeWithNoShift = Employee.builder().id(1L).build();
            Attendance attendance = Attendance.builder()
                    .employee(employeeWithNoShift)
                    .timeIn(LocalDateTime.of(MON, LocalTime.of(11, 0)))
                    .timeOut(LocalDateTime.of(MON, LocalTime.of(14, 0)))
                    .build();
            when(repository.findByEmployee_IdAndTimeInBetween(any(), any(), any())).thenReturn(List.of(attendance));

            AttendancePayrollSummary result = service.getAttendanceSummary(1L, MON, MON);

            assertThat(result.getTardinessMinutes()).isZero();
            assertThat(result.getUndertimeMinutes()).isZero();
        }
    }
}