package com.iodsky.mysweldo.attendance;

import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeService employeeService;
    @Mock private UserService userService;
    @InjectMocks private AttendanceService attendanceService;

    private User hrUser;
    private User normalUser;
    private Employee currentEmployee;
    private Employee otherEmployee;
    private AttendanceDto dto;
    private Attendance attendance;

    private static final LocalDate TODAY = LocalDate.of(2025, 11, 1);
    private static final LocalTime SHIFT_START = LocalTime.of(8, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(17, 0);
    private static final LocalTime PART_TIME_START = LocalTime.of(9, 0);
    private static final LocalTime PART_TIME_END = LocalTime.of(13, 0);

    @BeforeEach
    void setUp() {
        currentEmployee = new Employee();
        currentEmployee.setId(1L);
        currentEmployee.setStartShift(SHIFT_START);
        currentEmployee.setEndShift(SHIFT_END);

        otherEmployee = new Employee();
        otherEmployee.setId(2L);
        otherEmployee.setStartShift(PART_TIME_START);
        otherEmployee.setEndShift(PART_TIME_END);

        hrUser = new User();
        hrUser.setRole(new Role("HR"));
        hrUser.setEmployee(currentEmployee);

        normalUser = new User();
        normalUser.setRole(new Role("EMPLOYEE"));
        normalUser.setEmployee(currentEmployee);

        dto = AttendanceDto.builder()
                .id(UUID.randomUUID())
                .employeeId(1L)
                .date(TODAY)
                .timeIn(SHIFT_START)
                .build();

        attendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(currentEmployee)
                .date(TODAY)
                .timeIn(SHIFT_START)
                .timeOut(LocalTime.MIN)
                .totalHours(BigDecimal.ZERO)
                .overtime(BigDecimal.ZERO)
                .build();
    }

    @Nested
    class CreateAttendanceTests {
        @Test
        void shouldCreateAttendanceSuccessfullyForSelf() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            when(attendanceRepository.findByEmployee_IdAndDate(anyLong(), any())).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(anyLong())).thenReturn(currentEmployee);
            when(attendanceRepository.save(any())).thenReturn(attendance);

            Attendance result = attendanceService.createAttendance(dto);

            assertNotNull(result);
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        void shouldAllowHrToCreateAttendanceForOthers() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            dto.setEmployeeId(otherEmployee.getId());
            when(attendanceRepository.findByEmployee_IdAndDate(anyLong(), any())).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(anyLong())).thenReturn(otherEmployee);
            when(attendanceRepository.save(any())).thenReturn(attendance);

            Attendance result = attendanceService.createAttendance(dto);

            assertNotNull(result);
            verify(employeeService).getEmployeeById(otherEmployee.getId());
        }

        @Test
        void shouldThrowUnauthorizedWhenPrincipalIsNotUser() {
            when(userService.getAuthenticatedUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.createAttendance(dto));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldThrowForbiddenWhenNonHrCreatesForOthers() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            dto.setEmployeeId(otherEmployee.getId());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.createAttendance(dto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldAllowClockInAtAnyTime() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            dto.setTimeIn(LocalTime.of(3, 0));
            when(attendanceRepository.findByEmployee_IdAndDate(anyLong(), any())).thenReturn(Optional.empty());
            when(employeeService.getEmployeeById(anyLong())).thenReturn(currentEmployee);
            when(attendanceRepository.save(any())).thenReturn(attendance);

            Attendance result = attendanceService.createAttendance(dto);

            assertNotNull(result);
            verify(attendanceRepository).save(any(Attendance.class));
        }

        @Test
        void shouldThrowConflictWhenAttendanceAlreadyExists() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            when(attendanceRepository.findByEmployee_IdAndDate(anyLong(), any())).thenReturn(Optional.of(attendance));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.createAttendance(dto));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }
    }

    @Nested
    class UpdateAttendanceTests {
        @Test
        void shouldAllowEmployeeToClockOutSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.MIN)
                    .build();

            LocalTime clockOutTime = SHIFT_START.plusHours(8);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any())).thenReturn(existing);

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(clockOutTime);
                Attendance result = attendanceService.updateAttendance(existing.getId(), null);

                assertNotNull(result.getTimeOut());
                assertEquals(clockOutTime, result.getTimeOut());
                verify(attendanceRepository).save(existing);
            }
        }

        @Test
        void shouldThrowConflictWhenAlreadyClockedOut() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.of(17, 0))
                    .build();

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.updateAttendance(existing.getId(), null));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenClockOutBeforeTimeIn() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(LocalTime.now())
                    .timeOut(LocalTime.MIN)
                    .build();

            LocalTime earlierTime = existing.getTimeIn().minusHours(1);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(earlierTime);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                        () -> attendanceService.updateAttendance(existing.getId(), null));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            }
        }

        @Test
        void shouldAllowHrToUpdateAttendanceManually() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.MIN)
                    .build();

            dto.setTimeOut(LocalTime.of(18, 0));

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any())).thenReturn(existing);

            Attendance result = attendanceService.updateAttendance(existing.getId(), dto);

            assertEquals(dto.getTimeOut(), result.getTimeOut());
            verify(attendanceRepository).save(existing);
        }

        @Test
        void shouldThrowForbiddenWhenNonHrTriesToEditAttendanceManually() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.MIN)
                    .build();

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.updateAttendance(existing.getId(), dto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenAttendanceDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.updateAttendance(UUID.randomUUID(), dto));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldCalculateOvertimeBasedOnEmployeeShift() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.MIN)
                    .build();

            // Clock out at 19:00 (11 hours worked, 9-hour shift = 2 hours overtime)
            LocalTime clockOutTime = LocalTime.of(19, 0);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any())).thenReturn(existing);

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(clockOutTime);
                Attendance result = attendanceService.updateAttendance(existing.getId(), null);

                assertEquals(clockOutTime, result.getTimeOut());
                assertEquals(new BigDecimal("11.00"), result.getTotalHours());
                assertEquals(new BigDecimal("2.00"), result.getOvertime());
            }
        }

        @Test
        void shouldCalculateNoOvertimeWhenWorkingLessThanShiftHours() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(currentEmployee)
                    .date(TODAY)
                    .timeIn(SHIFT_START) // 08:00
                    .timeOut(LocalTime.MIN)
                    .build();

            // Clock out at 15:00 (7 hours worked, 9-hour shift = 0 overtime)
            LocalTime clockOutTime = LocalTime.of(15, 0);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any())).thenReturn(existing);

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(clockOutTime);
                Attendance result = attendanceService.updateAttendance(existing.getId(), null);

                assertEquals(new BigDecimal("7.00"), result.getTotalHours());
                assertEquals(0, result.getOvertime().compareTo(BigDecimal.ZERO));
            }
        }

        @Test
        void shouldCalculateOvertimeForPartTimeEmployee() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            normalUser.setEmployee(otherEmployee); // Part-time employee with 4-hour shift

            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(otherEmployee)
                    .date(TODAY)
                    .timeIn(PART_TIME_START) // 09:00
                    .timeOut(LocalTime.MIN)
                    .build();

            // Clock out at 15:00 (6 hours worked, 4-hour shift = 2 hours overtime)
            LocalTime clockOutTime = LocalTime.of(15, 0);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));
            when(attendanceRepository.save(any())).thenReturn(existing);

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(clockOutTime);
                Attendance result = attendanceService.updateAttendance(existing.getId(), null);

                assertEquals(new BigDecimal("6.00"), result.getTotalHours());
                assertEquals(new BigDecimal("2.00"), result.getOvertime());
            }
        }

        @Test
        void shouldThrowBadRequestWhenEmployeeHasNoShiftConfiguration() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Employee employeeWithoutShift = new Employee();
            employeeWithoutShift.setId(3L);

            Attendance existing = Attendance.builder()
                    .id(UUID.randomUUID())
                    .employee(employeeWithoutShift)
                    .date(TODAY)
                    .timeIn(SHIFT_START)
                    .timeOut(LocalTime.MIN)
                    .build();

            LocalTime clockOutTime = LocalTime.of(17, 0);
            normalUser.setEmployee(employeeWithoutShift);

            when(attendanceRepository.findById(any(UUID.class))).thenReturn(Optional.of(existing));

            try (MockedStatic<LocalTime> mocked = mockStatic(LocalTime.class, CALLS_REAL_METHODS)) {
                mocked.when(LocalTime::now).thenReturn(clockOutTime);

                ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                        () -> attendanceService.updateAttendance(existing.getId(), null));

                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
                assertTrue(ex.getReason().contains("shift times are not configured"));
            }
        }
    }

    @Nested
    class GetAllAttendancesTests {
        @Test
        void shouldReturnAllAttendancesForSpecificDate() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(List.of(attendance), pageable, 1);

            LocalDate expectedEndDate = TODAY.withDayOfMonth(TODAY.lengthOfMonth());
            when(attendanceRepository.findAllByDateBetween(eq(TODAY), eq(expectedEndDate), any(Pageable.class)))
                    .thenReturn(attendancePage);

            Page<Attendance> result = attendanceService.getAllAttendances(0, 10, TODAY, null);

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            verify(attendanceRepository).findAllByDateBetween(eq(TODAY), eq(expectedEndDate), any(Pageable.class));
        }

        @Test
        void shouldReturnAttendancesForDateRange() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(List.of(attendance), pageable, 1);
            when(attendanceRepository.findAllByDateBetween(any(), any(), any(Pageable.class))).thenReturn(attendancePage);

            Page<Attendance> result = attendanceService.getAllAttendances(0, 10, TODAY, TODAY.plusDays(2));

            assertFalse(result.isEmpty());
            assertEquals(1, result.getContent().size());
        }

    }

    @Nested
    class GetEmployeeAttendancesTests {
        @Test
        void shouldAllowEmployeeToGetOwnAttendances() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(List.of(attendance), pageable, 1);
            when(attendanceRepository.findByEmployee_IdAndDateBetween(anyLong(), any(), any(), any(Pageable.class)))
                    .thenReturn(attendancePage);

            Page<Attendance> result = attendanceService.getEmployeeAttendances(0, 10, null, TODAY, TODAY.plusDays(1));

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
        }

        @Test
        void shouldAllowHrToGetAnyEmployeeAttendances() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            Pageable pageable = PageRequest.of(0, 10);
            Page<Attendance> attendancePage = new PageImpl<>(List.of(attendance), pageable, 1);
            when(attendanceRepository.findByEmployee_IdAndDateBetween(anyLong(), any(), any(), any(Pageable.class)))
                    .thenReturn(attendancePage);

            Page<Attendance> result = attendanceService.getEmployeeAttendances(0, 10, otherEmployee.getId(), TODAY, TODAY.plusDays(1));

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
        }

        @Test
        void shouldThrowForbiddenWhenNonHrRequestsOthersData() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.getEmployeeAttendances(0, 10, otherEmployee.getId(), TODAY, TODAY.plusDays(1)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowUnauthorizedWhenNoAuth() {
            when(userService.getAuthenticatedUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> attendanceService.getEmployeeAttendances(0, 10, 1L, TODAY, TODAY.plusDays(1)));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldSupportNonPaginatedVersionForBackwardCompatibility() {
            when(attendanceRepository.findByEmployee_IdAndDateBetween(anyLong(), any(), any()))
                    .thenReturn(List.of(attendance));

            List<Attendance> result = attendanceService.getEmployeeAttendances(1L, TODAY, TODAY.plusDays(1));

            assertEquals(1, result.size());
            verify(attendanceRepository).findByEmployee_IdAndDateBetween(eq(1L), eq(TODAY), eq(TODAY.plusDays(1)));
        }
    }

}
