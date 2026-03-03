package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.common.RequestStatus;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class OvertimeRequestServiceTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private UserService userService;
    @Mock
    private AttendanceService attendanceService;
    @Mock
    private OvertimeRequestRepository repository;

    @InjectMocks private OvertimeRequestService service;

    private Employee hrEmployee;
    private Employee employee;
    private Employee supervisorEmployee;
    private Employee subordinateEmployee;

    private User hrUser;
    private User employeeUser;
    private User supervisorUser;

    private Attendance validAttendance;
    private Attendance invalidAttendance;

    private OvertimeRequest pendingOvertimeRequest;
    private OvertimeRequest approvedOvertimeRequest;
    private OvertimeRequest rejectedOvertimeRequest;
    private OvertimeRequest otherEmployeeRequest;
    private OvertimeRequest subordinateOvertimeRequest;

    private AddOvertimeRequest addOvertimeRequestByEmployee;
    private AddOvertimeRequest addOvertimeRequestByHR;
    private UpdateOvertimeRequest updateOvertimeRequestDto;

    private Pageable pageable;
    private Page<OvertimeRequest> pageWithAllRequests;
    private Page<OvertimeRequest> pageWithFilteredRequests;
    private Page<OvertimeRequest> emptyPage;

    @BeforeEach
    void setup() {
        hrEmployee = Employee.builder()
                .id(10000L)
                .build();

        employee = Employee.builder()
                .id(10001L)
                .build();

        supervisorEmployee = Employee.builder()
                .id(10002L)
                .build();

        subordinateEmployee = Employee.builder()
                .id(10003L)
                .supervisor(supervisorEmployee)
                .build();

        hrUser = User.builder()
                .id(UUID.randomUUID())
                .employee(hrEmployee)
                .role(new Role("HR"))
                .build();

        employeeUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(new Role("EMPLOYEE"))
                .build();

        supervisorUser = User.builder()
                .id(UUID.randomUUID())
                .employee(supervisorEmployee)
                .role(new Role("SUPERVISOR"))
                .build();

        validAttendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .date(LocalDate.of(2026, 1, 1))
                .timeIn(LocalTime.of(8, 0))
                .timeOut(LocalTime.of(18, 12))
                .totalHours(BigDecimal.valueOf(10.2))
                .overtime(BigDecimal.valueOf(2.2))
                .build();

        invalidAttendance = Attendance.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .date(LocalDate.of(2026, 1, 2))
                .timeIn(LocalTime.of(8, 0))
                .timeOut(LocalTime.of(17, 0))
                .totalHours(BigDecimal.valueOf(9))
                .overtime(BigDecimal.ZERO)
                .build();

        pendingOvertimeRequest = OvertimeRequest.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2026, 1, 1))
                .employee(employee)
                .status(RequestStatus.PENDING)
                .overtimeHours(BigDecimal.valueOf(2.2))
                .reason("Need to complete urgent project tasks")
                .build();

        approvedOvertimeRequest = OvertimeRequest.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2026, 1, 3))
                .employee(employee)
                .status(RequestStatus.APPROVED)
                .overtimeHours(BigDecimal.valueOf(3.0))
                .reason("Approved overtime for project deadline")
                .build();

        rejectedOvertimeRequest = OvertimeRequest.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2026, 1, 4))
                .employee(employee)
                .status(RequestStatus.REJECTED)
                .overtimeHours(BigDecimal.valueOf(1.5))
                .reason("Rejected overtime request")
                .build();

        otherEmployeeRequest = OvertimeRequest.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2026, 1, 5))
                .employee(hrEmployee)
                .status(RequestStatus.PENDING)
                .overtimeHours(BigDecimal.valueOf(2.0))
                .reason("Other employee's overtime request")
                .build();

        subordinateOvertimeRequest = OvertimeRequest.builder()
                .id(UUID.randomUUID())
                .date(LocalDate.of(2026, 1, 6))
                .employee(subordinateEmployee)
                .status(RequestStatus.PENDING)
                .overtimeHours(BigDecimal.valueOf(3.5))
                .reason("Subordinate's overtime request")
                .build();

        addOvertimeRequestByEmployee = AddOvertimeRequest.builder()
                .date(LocalDate.of(2026, 1, 1))
                .reason("Need to complete urgent project tasks")
                .build();

        addOvertimeRequestByHR = AddOvertimeRequest.builder()
                .date(LocalDate.of(2026, 1, 1))
                .employeeId(10001L)
                .reason("HR creating overtime request on behalf of employee")
                .build();

        updateOvertimeRequestDto = UpdateOvertimeRequest.builder()
                .date(LocalDate.of(2026, 1, 2))
                .reason("Updated reason for overtime")
                .build();

        pageable = PageRequest.of(0, 10);
        pageWithAllRequests = new PageImpl<>(List.of(pendingOvertimeRequest, approvedOvertimeRequest, rejectedOvertimeRequest), pageable, 3);
        pageWithFilteredRequests = new PageImpl<>(List.of(approvedOvertimeRequest, rejectedOvertimeRequest), pageable, 2);
        emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Nested
    class CreateOvertimeRequestTests {

        @Test
        void shouldCreateOvertimeSuccessfullyForSelf() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(employeeService.getEmployeeById(anyLong())).thenReturn(employee);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any())).thenReturn(validAttendance);
            when(repository.save(any(OvertimeRequest.class))).thenReturn(pendingOvertimeRequest);

            OvertimeRequest result = service.createOvertimeRequest(addOvertimeRequestByEmployee);

            assertNotNull(result);
            assertEquals(RequestStatus.PENDING, result.getStatus());
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldCreateOvertimeRequestSuccessfullyForOtherEmployeeByHR() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(employeeService.getEmployeeById(anyLong())).thenReturn(employee);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any())).thenReturn(validAttendance);
            when(repository.save(any())).thenReturn(pendingOvertimeRequest);

            OvertimeRequest result = service.createOvertimeRequest(addOvertimeRequestByHR);

            assertNotNull(result);
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowConflictWhenOvertimeRequestAlreadyExists() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createOvertimeRequest(addOvertimeRequestByEmployee));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowForbiddenWhenNonHRCreatesForOtherEmployee() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createOvertimeRequest(addOvertimeRequestByHR));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verifyNoInteractions(repository);
        }

        @Test
        void shouldThrowNotFoundWhenAttendanceDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(employeeService.getEmployeeById(anyLong())).thenReturn(employee);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createOvertimeRequest(addOvertimeRequestByEmployee));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowBadRequestWhenNoOvertimeHours() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(employeeService.getEmployeeById(anyLong())).thenReturn(employee);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any()))
                    .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.createOvertimeRequest(addOvertimeRequestByEmployee));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

    }

    @Nested
    class GetOvertimeRequestsTests {

        @Test
        void shouldReturnAllOvertimeRequestsWithPagination() {
            when(repository.findAll(any(Pageable.class))).thenReturn(pageWithAllRequests);

            Page<OvertimeRequest> result = service.getOvertimeRequests(null, null, 0, 10);

            assertEquals(3, result.getTotalElements());
            verify(repository).findAll(any(Pageable.class));
        }

        @Test
        void shouldReturnOvertimeRequestsFilteredByDateRange() {
            when(repository.findByDateBetween(any(), any(), any(Pageable.class))).thenReturn(pageWithFilteredRequests);

            LocalDate startDate = LocalDate.of(2026, 1, 3);
            LocalDate endDate = LocalDate.of(2026, 1, 4);
            Page<OvertimeRequest> result = service.getOvertimeRequests(startDate, endDate, 0, 10);

            assertEquals(2, result.getTotalElements());
            verify(repository).findByDateBetween(any(), any(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoRequestsExist() {
            when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<OvertimeRequest> result = service.getOvertimeRequests(null, null, 0, 10);

            assertEquals(0, result.getTotalElements());
            verify(repository).findAll(any(Pageable.class));
        }

    }

    @Nested
    class GetEmployeeOvertimeRequestTests {

        @Test
        void shouldReturnAuthenticatedEmployeeOvertimeRequestsWithPagination() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findAllByEmployee_Id(anyLong(), any(Pageable.class))).thenReturn(pageWithAllRequests);

            Page<OvertimeRequest> result = service.getEmployeeOvertimeRequest(0, 10);

            assertEquals(3, result.getTotalElements());
            verify(repository).findAllByEmployee_Id(anyLong(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenEmployeeHasNoRequests() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findAllByEmployee_Id(anyLong(), any(Pageable.class))).thenReturn(emptyPage);

            Page<OvertimeRequest> result = service.getEmployeeOvertimeRequest(0, 10);

            assertEquals(0, result.getTotalElements());
            verify(repository).findAllByEmployee_Id(anyLong(), any(Pageable.class));
        }

    }

    @Nested
    class GetSubordinatesOvertimeRequestsTest {

        @Test
        void shouldReturnSubordinatesOvertimeRequestsWithPagination() {
            Page<OvertimeRequest> subordinatesPage = new PageImpl<>(List.of(subordinateOvertimeRequest), pageable, 1);
            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class)))
                    .thenReturn(subordinatesPage);

            Page<OvertimeRequest> result = service.getSubordinatesOvertimeRequests(0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(subordinateOvertimeRequest.getId(), result.getContent().get(0).getId());
            verify(repository).findByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoSubordinateRequests() {
            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<OvertimeRequest> result = service.getSubordinatesOvertimeRequests(0, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            verify(repository).findByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class));
        }

    }

    @Nested
    class GetOvertimeRequestByIdTests {

        @Test
        void shouldReturnOvertimeRequestByIdForOwner() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));

            OvertimeRequest result = service.getOvertimeRequestById(pendingOvertimeRequest.getId());

            assertNotNull(result);
            verify(repository).findById(any(UUID.class));
        }

        @Test
        void shouldReturnOvertimeRequestByIdForHR() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(rejectedOvertimeRequest));

            OvertimeRequest result = service.getOvertimeRequestById(rejectedOvertimeRequest.getId());

            assertNotNull(result);
            verify(repository).findById(any(UUID.class));
        }

        @Test
        void shouldThrowNotFoundWhenOvertimeRequestDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(any(UUID.class))).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getOvertimeRequestById(rejectedOvertimeRequest.getId()));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowForbiddenWhenNonHRAccessesOtherEmployeeRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(otherEmployeeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.getOvertimeRequestById(otherEmployeeRequest.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

    }

    @Nested
    class UpdateOvertimeRequestTests {

        @Test
        void shouldUpdateOvertimeRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any())).thenReturn(validAttendance);
            when(repository.save(any(OvertimeRequest.class))).thenReturn(pendingOvertimeRequest);

            OvertimeRequest result = service.updateOvertimeRequest(pendingOvertimeRequest.getId(), updateOvertimeRequestDto);

            assertNotNull(result);
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldUpdateOnlyReasonWithoutChangingDate() {
            UpdateOvertimeRequest sameDateUpdate = UpdateOvertimeRequest.builder()
                    .date(LocalDate.of(2026, 1, 1))
                    .reason("Updated reason only")
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.save(any(OvertimeRequest.class))).thenReturn(pendingOvertimeRequest);

            OvertimeRequest result = service.updateOvertimeRequest(pendingOvertimeRequest.getId(), sameDateUpdate);

            assertNotNull(result);
            verify(attendanceService, never()).getEmployeeAttendanceByDate(anyLong(), any());
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldUpdateDateAndRefreshOvertimeHours() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(false);
            when(attendanceService.getEmployeeAttendanceByDate(anyLong(), any())).thenReturn(validAttendance);
            when(repository.save(any(OvertimeRequest.class))).thenReturn(pendingOvertimeRequest);

            OvertimeRequest result = service.updateOvertimeRequest(pendingOvertimeRequest.getId(), updateOvertimeRequestDto);

            assertNotNull(result);
            verify(attendanceService).getEmployeeAttendanceByDate(anyLong(), any());
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowConflictWhenUpdatingToExistingDate() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.existsByEmployee_IdAndDate(anyLong(), any())).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateOvertimeRequest(pendingOvertimeRequest.getId(), updateOvertimeRequestDto));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertTrue(ex.getReason().contains("already exists"));
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowBadRequestWhenUpdatingNonPendingRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(approvedOvertimeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateOvertimeRequest(approvedOvertimeRequest.getId(), updateOvertimeRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Cannot update"));
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowForbiddenWhenNonHRUpdatesOtherEmployeeRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(otherEmployeeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateOvertimeRequest(otherEmployeeRequest.getId(), updateOvertimeRequestDto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

    }

    @Nested
    class UpdateOvertimeRequestStatusTests {

        @Test
        void shouldApproveOvertimeRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.save(any(OvertimeRequest.class))).thenReturn(approvedOvertimeRequest);

            OvertimeRequest result = service.updateOvertimeRequestStatus(pendingOvertimeRequest.getId(), RequestStatus.APPROVED);

            assertNotNull(result);
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldRejectOvertimeRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.save(any(OvertimeRequest.class))).thenReturn(rejectedOvertimeRequest);

            OvertimeRequest result = service.updateOvertimeRequestStatus(pendingOvertimeRequest.getId(), RequestStatus.REJECTED);

            assertNotNull(result);
            verify(repository).save(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowForbiddenWhenNonHRUpdatesStatus() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(otherEmployeeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.updateOvertimeRequestStatus(otherEmployeeRequest.getId(), RequestStatus.APPROVED));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
        }

    }

    @Nested
    class DeleteOvertimeRequestTests {

        @Test
        void shouldSoftDeleteOvertimeRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(pendingOvertimeRequest));
            when(repository.save(any(OvertimeRequest.class))).thenReturn(pendingOvertimeRequest);

            service.deleteOvertimeRequest(pendingOvertimeRequest.getId());

            verify(repository).save(any(OvertimeRequest.class));
            verify(repository, never()).delete(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowBadRequestWhenDeletingNonPendingRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(approvedOvertimeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.deleteOvertimeRequest(approvedOvertimeRequest.getId()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Cannot delete"));
            verify(repository, never()).save(any(OvertimeRequest.class));
            verify(repository, never()).delete(any(OvertimeRequest.class));
        }

        @Test
        void shouldThrowForbiddenWhenNonHRDeletesOtherEmployeeRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(any(UUID.class))).thenReturn(Optional.of(otherEmployeeRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.deleteOvertimeRequest(otherEmployeeRequest.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
            verify(repository, never()).save(any(OvertimeRequest.class));
            verify(repository, never()).delete(any(OvertimeRequest.class));
        }

    }

}