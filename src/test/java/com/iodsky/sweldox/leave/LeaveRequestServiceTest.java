package com.iodsky.sweldox.leave;

import com.iodsky.sweldox.common.RequestStatus;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.leave.credit.LeaveCredit;
import com.iodsky.sweldox.leave.credit.LeaveCreditService;
import com.iodsky.sweldox.leave.request.*;
import com.iodsky.sweldox.security.user.User;
import com.iodsky.sweldox.security.user.UserRole;
import com.iodsky.sweldox.security.user.UserService;
import jakarta.persistence.OptimisticLockException;
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock private LeaveRequestRepository repository;
    @Mock private LeaveCreditService leaveCreditService;
    @Mock private LeaveRequestMapper mapper;
    @Mock private UserService userService;
    @InjectMocks private LeaveRequestService service;

    private Employee hrEmployee;
    private Employee employee;
    private Employee supervisorEmployee;
    private Employee subordinateEmployee;

    private User hrUser;
    private User employeeUser;
    private User supervisorUser;

    private LeaveRequest pendingLeaveRequest;
    private LeaveRequest approvedLeaveRequest;
    private LeaveRequest rejectedLeaveRequest;
    private LeaveRequest otherEmployeeLeaveRequest;
    private LeaveRequest subordinateLeaveRequest;

    private LeaveRequestDto leaveRequestDto;
    private LeaveRequestDto updateLeaveRequestDto;

    private LeaveCredit leaveCredit;

    private Pageable pageable;
    private Page<LeaveRequest> pageWithAllRequests;
    private Page<LeaveRequest> pageWithFilteredRequests;
    private Page<LeaveRequest> emptyPage;

    @BeforeEach
    void setup() {
        hrEmployee = Employee.builder()
                .id(10000L)
                .firstName("Maria")
                .lastName("Santos")
                .build();

        employee = Employee.builder()
                .id(10001L)
                .firstName("Juan")
                .lastName("Dela Cruz")
                .build();

        supervisorEmployee = Employee.builder()
                .id(10002L)
                .firstName("Pedro")
                .lastName("Garcia")
                .build();

        subordinateEmployee = Employee.builder()
                .id(10003L)
                .firstName("Ana")
                .lastName("Reyes")
                .supervisor(supervisorEmployee)
                .build();

        hrUser = User.builder()
                .id(UUID.randomUUID())
                .employee(hrEmployee)
                .userRole(new UserRole("HR"))
                .build();

        employeeUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .userRole(new UserRole("EMPLOYEE"))
                .build();

        supervisorUser = User.builder()
                .id(UUID.randomUUID())
                .employee(supervisorEmployee)
                .userRole(new UserRole("SUPERVISOR"))
                .build();

        leaveRequestDto = LeaveRequestDto.builder()
                .leaveType("VACATION")
                .startDate(LocalDate.of(2025, 12, 16)) // Monday
                .endDate(LocalDate.of(2025, 12, 19)) // Thursday
                .note("Year end vacation")
                .build();

        updateLeaveRequestDto = LeaveRequestDto.builder()
                .leaveType("VACATION")
                .startDate(LocalDate.of(2025, 12, 23)) // Monday
                .endDate(LocalDate.of(2025, 12, 26)) // Thursday
                .note("Updated vacation dates")
                .build();

        leaveCredit = LeaveCredit.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .type(LeaveType.VACATION)
                .credits(10.0)
                .build();

        pendingLeaveRequest = LeaveRequest.builder()
                .id("LR-2025-001")
                .employee(employee)
                .leaveType(LeaveType.VACATION)
                .startDate(LocalDate.of(2025, 12, 16))
                .endDate(LocalDate.of(2025, 12, 19))
                .note("Pending vacation leave")
                .status(RequestStatus.PENDING)
                .build();

        approvedLeaveRequest = LeaveRequest.builder()
                .id("LR-2025-002")
                .employee(employee)
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.of(2025, 11, 10))
                .endDate(LocalDate.of(2025, 11, 12))
                .note("Approved sick leave")
                .status(RequestStatus.APPROVED)
                .build();

        rejectedLeaveRequest = LeaveRequest.builder()
                .id("LR-2025-003")
                .employee(employee)
                .leaveType(LeaveType.VACATION)
                .startDate(LocalDate.of(2025, 10, 5))
                .endDate(LocalDate.of(2025, 10, 7))
                .note("Rejected vacation leave")
                .status(RequestStatus.REJECTED)
                .build();

        otherEmployeeLeaveRequest = LeaveRequest.builder()
                .id("LR-2025-004")
                .employee(hrEmployee)
                .leaveType(LeaveType.VACATION)
                .startDate(LocalDate.of(2025, 12, 20))
                .endDate(LocalDate.of(2025, 12, 23))
                .note("Other employee's leave request")
                .status(RequestStatus.PENDING)
                .build();

        subordinateLeaveRequest = LeaveRequest.builder()
                .id("LR-2025-005")
                .employee(subordinateEmployee)
                .leaveType(LeaveType.SICK)
                .startDate(LocalDate.of(2025, 12, 27))
                .endDate(LocalDate.of(2025, 12, 30))
                .note("Subordinate's leave request")
                .status(RequestStatus.PENDING)
                .build();

        pageable = PageRequest.of(0, 10);
        pageWithAllRequests = new PageImpl<>(List.of(pendingLeaveRequest, approvedLeaveRequest, rejectedLeaveRequest), pageable, 3);
        pageWithFilteredRequests = new PageImpl<>(List.of(pendingLeaveRequest, approvedLeaveRequest), pageable, 2);
        emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    @Nested
    class CreateLeaveRequestTests {

        @Test
        void shouldCreateLeaveRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(eq(10001L), eq(LeaveType.VACATION)))
                    .thenReturn(leaveCredit);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(eq(10001L), any(), any()))
                    .thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(10001L), anyList(), any(), any()))
                    .thenReturn(false);
            when(repository.save(any(LeaveRequest.class))).thenReturn(pendingLeaveRequest);

            LeaveRequest result = service.createLeaveRequest(leaveRequestDto);

            assertNotNull(result);
            assertEquals(RequestStatus.PENDING, result.getStatus());
            verify(repository).save(any(LeaveRequest.class));
        }

        @Test
        void shouldThrowUnauthorizedWhenPrincipalIsNotUser() {
            when(userService.getAuthenticatedUser()).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenInsufficientLeaveCredits() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            leaveCredit.setCredits(2.0); // Only 2 days available, but 4 days required
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(eq(10001L), eq(LeaveType.VACATION)))
                    .thenReturn(leaveCredit);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(eq(10001L), any(), any()))
                    .thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(10001L), anyList(), any(), any()))
                    .thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenStartDateIsAfterEndDate() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            leaveRequestDto.setStartDate(LocalDate.of(2025, 12, 20));
            leaveRequestDto.setEndDate(LocalDate.of(2025, 12, 15));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenStartDateIsWeekend() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            leaveRequestDto.setStartDate(LocalDate.of(2025, 12, 13)); // Saturday
            leaveRequestDto.setEndDate(LocalDate.of(2025, 12, 16));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("Start date must be a weekday", ex.getReason());
        }

        @Test
        void shouldThrowBadRequestWhenEndDateIsWeekend() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            leaveRequestDto.setStartDate(LocalDate.of(2025, 12, 16));
            leaveRequestDto.setEndDate(LocalDate.of(2025, 12, 20)); // Saturday

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("End date must be a weekday", ex.getReason());
        }

        @Test
        void shouldThrowConflictWhenDuplicateLeaveRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(eq(10001L), any(), any()))
                    .thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertEquals("Duplicate leave request", ex.getReason());
        }

        @Test
        void shouldThrowBadRequestWhenOverlappingLeaveExists() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(eq(10001L), any(), any()))
                    .thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(10001L), anyList(), any(), any()))
                    .thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("overlaps"));
        }

        @Test
        void shouldThrowBadRequestWhenInvalidLeaveType() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            leaveRequestDto.setLeaveType("INVALID_TYPE");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.createLeaveRequest(leaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Invalid leave type"));
        }
    }

    @Nested
    class GetLeaveRequestsTests {

       @Test
       void shouldReturnAllLeaveRequestsWithPagination() {
           when(repository.findAll(any(Pageable.class))).thenReturn(pageWithAllRequests);

           Page<LeaveRequest> result = service.getLeaveRequests(null, null, 0, 10);

           assertEquals(3, result.getTotalElements());
           verify(repository).findAll(any(Pageable.class));
       }

        @Test
        void shouldReturnLeaveRequestsFilteredByDateRange() {
            when(repository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any(Pageable.class)))
                    .thenReturn(pageWithFilteredRequests);

            LocalDate startDate = LocalDate.of(2025, 11, 1);
            LocalDate endDate = LocalDate.of(2025, 12, 31);
            Page<LeaveRequest> result = service.getLeaveRequests(startDate, endDate, 0, 10);

            assertEquals(2, result.getTotalElements());
            verify(repository).findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoRequestsExist() {
            when(repository.findAll(any(Pageable.class))).thenReturn(emptyPage);

            Page<LeaveRequest> result = service.getLeaveRequests(null, null, 0, 10);

            assertEquals(0, result.getTotalElements());
            verify(repository).findAll(any(Pageable.class));
        }

    }

    @Nested
    class GetEmployeeLeaveRequestsTests {

        @Test
        void shouldReturnAuthenticatedEmployeeLeaveRequestsWithPagination() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findAllByEmployee_Id(anyLong(), any(Pageable.class))).thenReturn(pageWithAllRequests);

            Page<LeaveRequest> result = service.getEmployeeLeaveRequests(0, 10);

            assertEquals(3, result.getTotalElements());
            verify(repository).findAllByEmployee_Id(anyLong(), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenEmployeeHasNoRequests() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findAllByEmployee_Id(anyLong(), any(Pageable.class))).thenReturn(emptyPage);

            Page<LeaveRequest> result = service.getEmployeeLeaveRequests(0, 10);

            assertEquals(0, result.getTotalElements());
            verify(repository).findAllByEmployee_Id(anyLong(), any(Pageable.class));
        }

    }

    @Nested
    class GetSubordinatesLeaveRequestsTests {

        @Test
        void shouldReturnSubordinatesLeaveRequestsWithPagination() {
            Page<LeaveRequest> subordinatesPage = new PageImpl<>(List.of(subordinateLeaveRequest), pageable, 1);
            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findAllByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class)))
                    .thenReturn(subordinatesPage);

            Page<LeaveRequest> result = service.getSubordinatesLeaveRequests(0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            assertEquals(subordinateLeaveRequest.getId(), result.getContent().get(0).getId());
            verify(repository).findAllByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class));
        }

        @Test
        void shouldReturnEmptyPageWhenNoSubordinateRequests() {
            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findAllByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class)))
                    .thenReturn(emptyPage);

            Page<LeaveRequest> result = service.getSubordinatesLeaveRequests(0, 10);

            assertNotNull(result);
            assertEquals(0, result.getTotalElements());
            verify(repository).findAllByEmployee_Supervisor_Id(eq(supervisorEmployee.getId()), any(Pageable.class));
        }

    }

    @Nested
    class GetLeaveRequestByIdTests {

        @Test
        void shouldReturnLeaveRequestByIdForOwner() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(pendingLeaveRequest));

            LeaveRequest result = service.getLeaveRequestById(pendingLeaveRequest.getId());

            assertNotNull(result);
            verify(repository).findById(anyString());
        }

        @Test
        void shouldReturnLeaveRequestByIdForHR() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(rejectedLeaveRequest));

            LeaveRequest result = service.getLeaveRequestById(rejectedLeaveRequest.getId());

            assertNotNull(result);
            verify(repository).findById(anyString());
        }

        @Test
        void shouldThrowNotFoundWhenLeaveRequestDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(anyString())).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.getLeaveRequestById("LR-2025-999"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

    }

    @Nested
    class UpdateLeaveRequestTests {

        @Test
        void shouldUpdateLeaveRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(pendingLeaveRequest.getId())).thenReturn(Optional.of(pendingLeaveRequest));
            when(mapper.updateEntity(any(LeaveRequest.class), any(LeaveRequestDto.class))).thenReturn(pendingLeaveRequest);
            when(repository.save(any(LeaveRequest.class))).thenReturn(pendingLeaveRequest);

            LeaveRequest result = service.updateLeaveRequest(pendingLeaveRequest.getId(), updateLeaveRequestDto);

            assertNotNull(result);
            verify(repository).save(any(LeaveRequest.class));
        }

        @Test
        void shouldAllowHRToUpdateOtherEmployeeLeaveRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(otherEmployeeLeaveRequest.getId())).thenReturn(Optional.of(otherEmployeeLeaveRequest));
            doReturn(otherEmployeeLeaveRequest).when(mapper).updateEntity(any(LeaveRequest.class), any(LeaveRequestDto.class));
            doReturn(otherEmployeeLeaveRequest).when(repository).save(any(LeaveRequest.class));

            LeaveRequest result = service.updateLeaveRequest(otherEmployeeLeaveRequest.getId(), updateLeaveRequestDto);

            assertNotNull(result);
            verify(repository).save(any(LeaveRequest.class));
        }

        @Test
        void shouldThrowForbiddenWhenNonHRUpdatesOtherEmployeeRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(otherEmployeeLeaveRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.updateLeaveRequest(otherEmployeeLeaveRequest.getId(), updateLeaveRequestDto));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenUpdatingProcessedRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById("LR-2025-002")).thenReturn(Optional.of(approvedLeaveRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.updateLeaveRequest("LR-2025-002", updateLeaveRequestDto));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Cannot delete processed"));
        }

    }

    @Nested
    class UpdateLeaveRequestStatusTests {

        @Test
        void shouldApproveLeaveRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(anyLong(), any(LeaveType.class)))
                    .thenReturn(leaveCredit);
            when(leaveCreditService.updateLeaveCredit(any(UUID.class), any(LeaveCredit.class)))
                    .thenReturn(leaveCredit);
            when(repository.save(any(LeaveRequest.class))).thenReturn(approvedLeaveRequest);

            LeaveRequest result = service.updateLeaveStatus(pendingLeaveRequest.getId(), RequestStatus.APPROVED);

            assertNotNull(result);
            verify(repository).save(any(LeaveRequest.class));
        }

        @Test
        void shouldRejectLeaveRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-2025-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(repository.save(any(LeaveRequest.class))).thenReturn(rejectedLeaveRequest);

            LeaveRequest result = service.updateLeaveStatus("LR-2025-001", RequestStatus.REJECTED);

            assertNotNull(result);
            verify(leaveCreditService, never()).updateLeaveCredit(any(), any());
            verify(repository).save(any(LeaveRequest.class));
        }

        @Test
        void shouldThrowBadRequestWhenAlreadyProcessed() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-2025-002")).thenReturn(Optional.of(approvedLeaveRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.updateLeaveStatus("LR-2025-002", RequestStatus.APPROVED));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("already been processed"));
        }

        @Test
        void shouldThrowBadRequestWhenInsufficientCreditsForApproval() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            leaveCredit.setCredits(2.0); // Only 2 days, but 4 days required
            when(repository.findById("LR-2025-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(anyLong(), any(LeaveType.class)))
                    .thenReturn(leaveCredit);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.updateLeaveStatus("LR-2025-001", RequestStatus.APPROVED));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Insufficient credits"));
        }

        @Test
        void shouldThrowBadRequestWhenOptimisticLockOccurs() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-2025-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(anyLong(), any(LeaveType.class)))
                    .thenReturn(leaveCredit);
            when(leaveCreditService.updateLeaveCredit(any(UUID.class), any(LeaveCredit.class)))
                    .thenReturn(leaveCredit);
            when(repository.save(any(LeaveRequest.class)))
                    .thenThrow(new OptimisticLockException());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.updateLeaveStatus("LR-2025-001", RequestStatus.APPROVED));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("modified by another process"));
        }

    }

    @Nested
    class DeleteLeaveRequestTests {

        @Test
        void shouldDeleteLeaveRequestSuccessfully() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(pendingLeaveRequest));

            service.deleteLeaveRequest(pendingLeaveRequest.getId());

            verify(repository).delete(pendingLeaveRequest);
        }

        @Test
        void shouldAllowHRToDeleteOtherEmployeeLeaveRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(otherEmployeeLeaveRequest));

            service.deleteLeaveRequest(otherEmployeeLeaveRequest.getId());

            verify(repository).delete(otherEmployeeLeaveRequest);
        }

        @Test
        void shouldThrowForbiddenWhenNonHRDeletesOtherEmployeeRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(otherEmployeeLeaveRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.deleteLeaveRequest(otherEmployeeLeaveRequest.getId()));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        void shouldThrowBadRequestWhenDeletingProcessedRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(employeeUser);
            when(repository.findById(anyString())).thenReturn(Optional.of(approvedLeaveRequest));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    service.deleteLeaveRequest(otherEmployeeLeaveRequest.getId()));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertTrue(ex.getReason().contains("Cannot delete processed"));
        }

    }
}
