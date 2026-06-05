package com.iodsky.mysweldo.leave;

import com.iodsky.mysweldo.common.RequestStatus;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.leave.credit.LeaveCredit;
import com.iodsky.mysweldo.leave.credit.LeaveCreditService;
import com.iodsky.mysweldo.leave.request.LeaveRequest;
import com.iodsky.mysweldo.leave.request.LeaveRequestDto;
import com.iodsky.mysweldo.leave.request.LeaveRequestMapper;
import com.iodsky.mysweldo.leave.request.LeaveRequestRepository;
import com.iodsky.mysweldo.leave.request.LeaveRequestService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @InjectMocks
    private LeaveRequestService service;

    @Mock
    private LeaveRequestRepository repository;

    @Mock
    private LeaveCreditService leaveCreditService;

    @Mock
    private LeaveRequestMapper mapper;

    @Mock
    private UserService userService;

    private User hrUser;
    private User regularUser;
    private Employee employee;
    private Employee otherEmployee;

    // Monday–Friday (5 weekdays)
    private static final LocalDate MON = LocalDate.of(2026, 3, 9);
    private static final LocalDate FRI = LocalDate.of(2026, 3, 13);

    // Weekends
    private static final LocalDate SAT = LocalDate.of(2026, 3, 14);
    
    @BeforeEach
    void setUp() {
        Role hrRole = new Role("HR");
        Role employeeRole = new Role("EMPLOYEE");

        Employee supervisor = Employee.builder().id(99L).build();

        employee = Employee.builder().id(1L).supervisor(supervisor).build();
        otherEmployee = Employee.builder().id(2L).supervisor(supervisor).build();

        hrUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(hrRole)
                .build();

        regularUser = User.builder()
                .id(UUID.randomUUID())
                .employee(otherEmployee)
                .role(employeeRole)
                .build();
    }

    // ─── createLeaveRequest ───────────────────────────────────────────────────

    @Nested
    class CreateLeaveRequestTests {

        @Test
        void shouldCreateAndSaveLeaveRequestWhenInputsAreValid() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .note("Family trip")
                    .build();

            LeaveCredit credit = LeaveCredit.builder().credits(10.0).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(1L, MON, FRI)).thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(1L), any(), eq(FRI), eq(MON))).thenReturn(false);
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION)).thenReturn(credit);
            when(repository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

            LeaveRequest result = service.createLeaveRequest(dto);

            assertThat(result.getLeaveType()).isEqualTo(LeaveType.VACATION);
            assertThat(result.getStartDate()).isEqualTo(MON);
            assertThat(result.getEndDate()).isEqualTo(FRI);
            assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.getEmployee()).isEqualTo(employee);
        }

        @Test
        void shouldThrow400WhenLeaveTypeIsInvalid() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("NONEXISTENT")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenStartDateIsAfterEndDate() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(FRI)
                    .endDate(MON)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenStartDateFallsOnWeekend() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(SAT)
                    .endDate(SAT)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenEndDateFallsOnWeekend() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(SAT)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow409WhenDuplicateLeaveRequestExists() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(2L, MON, FRI)).thenReturn(true);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrow400WhenDatesOverlapWithExistingPendingOrApprovedLeave() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(2L, MON, FRI)).thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(2L), any(), eq(FRI), eq(MON))).thenReturn(true);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenRequiredDaysExceedAvailableCredits() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            // 5 weekdays required, only 3 credits available
            LeaveCredit credit = LeaveCredit.builder().credits(3.0).build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(2L, MON, FRI)).thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(2L), any(), eq(FRI), eq(MON))).thenReturn(false);
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(2L, LeaveType.VACATION)).thenReturn(credit);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow403WhenRegularUserCreatesLeaveRequestForAnotherUser() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .employeeId(1L)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createLeaveRequest(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

    }

    // ─── getLeaveRequests ─────────────────────────────────────────────────────

    @Nested
    class GetLeaveRequestsTests {

        @Test
        void shouldReturnAllLeaveRequestsPagedWhenNoDateFilterProvided() {
            Page<LeaveRequest> expected = new PageImpl<>(List.of());
            when(repository.findAll(any(Pageable.class))).thenReturn(expected);

            Page<LeaveRequest> result = service.getLeaveRequests(null, null, 0, 10);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldReturnFilteredLeaveRequestsWhenDateRangeIsProvided() {
            Page<LeaveRequest> expected = new PageImpl<>(List.of());
            when(repository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expected);

            Page<LeaveRequest> result = service.getLeaveRequests(MON, FRI, 0, 10);

            assertThat(result).isEqualTo(expected);
        }

        @Test
        void shouldApplyDateRangeNormalizationWhenOnlyStartDateIsProvided() {
            Page<LeaveRequest> expected = new PageImpl<>(List.of());
            when(repository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(expected);

            Page<LeaveRequest> result = service.getLeaveRequests(MON, null, 0, 10);

            assertThat(result).isEqualTo(expected);
        }
    }

    // ─── getEmployeeLeaveRequests ─────────────────────────────────────────────

    @Nested
    class GetEmployeeLeaveRequestsTests {

        @Test
        void shouldReturnOnlyAuthenticatedEmployeeLeaveRequestsPaged() {
            Page<LeaveRequest> expected = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findAllByEmployee_Id(eq(2L), any(Pageable.class))).thenReturn(expected);

            Page<LeaveRequest> result = service.getEmployeeLeaveRequests(0, 10);

            assertThat(result).isEqualTo(expected);
        }
    }

    // ─── getSubordinatesLeaveRequests ─────────────────────────────────────────

    @Nested
    class GetSubordinatesLeaveRequestsTests {

        @Test
        void shouldReturnLeaveRequestsForAllSubordinatesOfAuthenticatedUser() {
            Page<LeaveRequest> expected = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findAllByEmployee_Supervisor_Id(eq(1L), any(Pageable.class))).thenReturn(expected);

            Page<LeaveRequest> result = service.getSubordinatesLeaveRequests(0, 10);

            assertThat(result).isEqualTo(expected);
        }
    }

    // ─── getLeaveRequestById ──────────────────────────────────────────────────

    @Nested
    class GetLeaveRequestByIdTests {

        private LeaveRequest leaveRequest;

        @BeforeEach
        void setUp() {
            leaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.VACATION)
                    .startDate(MON)
                    .endDate(FRI)
                    .status(RequestStatus.PENDING)
                    .build();
        }

        @Test
        void shouldReturnLeaveRequestWhenRequesterIsOwner() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(leaveRequest));

            LeaveRequest result = service.getLeaveRequestById("LR-001");

            assertThat(result).isEqualTo(leaveRequest);
        }

        @Test
        void shouldReturnLeaveRequestWhenRequesterHasHrRole() {
            Employee anotherEmployee = Employee.builder().id(50L).supervisor(employee).build();
            LeaveRequest otherRequest = LeaveRequest.builder()
                    .employee(anotherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-002")).thenReturn(Optional.of(otherRequest));

            LeaveRequest result = service.getLeaveRequestById("LR-002");

            assertThat(result).isEqualTo(otherRequest);
        }

        @Test
        void shouldReturnLeaveRequestWhenRequesterIsEmployeesSupervisor() {
            // employee has supervisor with id=99; build user for that supervisor
            Employee supervisorEmployee = Employee.builder().id(99L).build();
            Role anyRole = new Role("EMPLOYEE");
            User supervisorUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(supervisorEmployee)
                    .role(anyRole)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(leaveRequest));

            LeaveRequest result = service.getLeaveRequestById("LR-001");

            assertThat(result).isEqualTo(leaveRequest);
        }

        @Test
        void shouldThrow404WhenLeaveRequestDoesNotExist() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById("MISSING")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLeaveRequestById("MISSING"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrow403WhenRequesterIsNeitherOwnerNorHrNorSupervisor() {
            // regularUser (employee id=2) tries to access employee id=1's leave
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(leaveRequest));

            assertThatThrownBy(() -> service.getLeaveRequestById("LR-001"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ─── updateLeaveRequest ───────────────────────────────────────────────────

    @Nested
    class UpdateLeaveRequestTests {

        private LeaveRequest pendingLeaveRequest;

        @BeforeEach
        void setUp() {
            pendingLeaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.VACATION)
                    .startDate(MON)
                    .endDate(FRI)
                    .status(RequestStatus.PENDING)
                    .build();
        }

        @Test
        void shouldUpdateAndSaveLeaveRequestWhenOwnerUpdatesAPendingRequest() {
            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("SICK")
                    .startDate(MON)
                    .endDate(MON)
                    .note("Feeling ill")
                    .build();

            LeaveRequest updated = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.SICK)
                    .startDate(MON)
                    .endDate(MON)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(mapper.updateEntity(pendingLeaveRequest, dto)).thenReturn(updated);
            when(repository.save(updated)).thenReturn(updated);

            LeaveRequest result = service.updateLeaveRequest("LR-001", dto);

            assertThat(result).isEqualTo(updated);
        }

        @Test
        void shouldThrow403WhenNonOwnerNonHrUserAttemptsUpdate() {
            // regularUser (id=2) tries to update employee id=1's leave
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("SICK")
                    .startDate(MON)
                    .endDate(MON)
                    .build();

            assertThatThrownBy(() -> service.updateLeaveRequest("LR-001", dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow400WhenLeaveRequestIsNotPending() {
            LeaveRequest approved = LeaveRequest.builder()
                    .employee(employee)
                    .status(RequestStatus.APPROVED)
                    .build();

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.updateLeaveRequest("LR-001", dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ─── updateLeaveStatus ────────────────────────────────────────────────────

    @Nested
    class UpdateLeaveStatusTests {

        private LeaveRequest pendingLeaveRequest;
        private LeaveCredit leaveCredit;
        private UUID leaveCreditId;

        @BeforeEach
        void setUp() {
            leaveCreditId = UUID.randomUUID();
            leaveCredit = LeaveCredit.builder()
                    .id(leaveCreditId)
                    .credits(10.0)
                    .build();

            pendingLeaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.VACATION)
                    .startDate(MON)
                    .endDate(FRI) // 5 weekdays
                    .status(RequestStatus.PENDING)
                    .build();
        }

        @Test
        void shouldApproveLeaveAndDeductWeekdaysFromCreditsWhenRequesterIsHr() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION)).thenReturn(leaveCredit);
            when(repository.save(pendingLeaveRequest)).thenReturn(pendingLeaveRequest);

            LeaveRequest result = service.updateLeaveStatus("LR-001", RequestStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(leaveCredit.getCredits()).isEqualTo(5.0); // 10 - 5 weekdays
            verify(leaveCreditService).updateLeaveCredit(eq(leaveCreditId), eq(leaveCredit));
        }

        @Test
        void shouldApproveLeaveWhenRequesterIsEmployeesSupervisor() {
            Employee supervisorEmployee = Employee.builder().id(99L).build();
            User supervisorUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(supervisorEmployee)
                    .role(new Role("EMPLOYEE"))
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION)).thenReturn(leaveCredit);
            when(repository.save(pendingLeaveRequest)).thenReturn(pendingLeaveRequest);

            LeaveRequest result = service.updateLeaveStatus("LR-001", RequestStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        }

        @Test
        void shouldRejectLeaveWithoutModifyingCreditsWhenRequesterIsHr() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(repository.save(pendingLeaveRequest)).thenReturn(pendingLeaveRequest);

            LeaveRequest result = service.updateLeaveStatus("LR-001", RequestStatus.REJECTED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
            verifyNoInteractions(leaveCreditService);
        }

        @Test
        void shouldThrow403WhenRequesterIsNeitherHrNorSupervisor() {
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));

            assertThatThrownBy(() -> service.updateLeaveStatus("LR-001", RequestStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow400WhenLeaveRequestIsAlreadyProcessed() {
            LeaveRequest approved = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.VACATION)
                    .startDate(MON)
                    .endDate(FRI)
                    .status(RequestStatus.APPROVED)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.updateLeaveStatus("LR-001", RequestStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenApprovingButEmployeeHasInsufficientCredits() {
            LeaveCredit insufficientCredit = LeaveCredit.builder().id(leaveCreditId).credits(2.0).build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION)).thenReturn(insufficientCredit);

            assertThatThrownBy(() -> service.updateLeaveStatus("LR-001", RequestStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenOptimisticLockExceptionOccursDuringSave() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION)).thenReturn(leaveCredit);
            when(repository.save(pendingLeaveRequest)).thenThrow(new OptimisticLockException());

            assertThatThrownBy(() -> service.updateLeaveStatus("LR-001", RequestStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ─── deleteLeaveRequest ───────────────────────────────────────────────────

    @Nested
    class DeleteLeaveRequestTests {

        private LeaveRequest pendingLeaveRequest;

        @BeforeEach
        void setUp() {
            pendingLeaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.VACATION)
                    .startDate(MON)
                    .endDate(FRI)
                    .status(RequestStatus.PENDING)
                    .build();
        }

        @Test
        void shouldSoftDeleteLeaveRequestWhenOwnerDeletesAPendingRequest() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));
            when(repository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

            service.deleteLeaveRequest("LR-001");

            assertThat(pendingLeaveRequest.getDeletedAt()).isNotNull();
            verify(repository).save(pendingLeaveRequest);
        }

        @Test
        void shouldSoftDeleteLeaveRequestWhenHrDeletesAPendingRequest() {
            LeaveRequest otherPending = LeaveRequest.builder()
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-002")).thenReturn(Optional.of(otherPending));
            when(repository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));

            service.deleteLeaveRequest("LR-002");

            assertThat(otherPending.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrow403WhenNonOwnerNonHrUserAttemptsDelete() {
            // regularUser (id=2) tries to delete employee id=1's leave
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(pendingLeaveRequest));

            assertThatThrownBy(() -> service.deleteLeaveRequest("LR-001"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow400WhenLeaveRequestIsNotPendingOnDelete() {
            LeaveRequest approved = LeaveRequest.builder()
                    .employee(employee)
                    .status(RequestStatus.APPROVED)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById("LR-001")).thenReturn(Optional.of(approved));

            assertThatThrownBy(() -> service.deleteLeaveRequest("LR-001"))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    // ─── calculateTotalDays (tested indirectly via createLeaveRequest) ────────

    @Nested
    class CalculateTotalDaysTests {

        private void stubValidationAndCreditCheck(Long empId, LocalDate start, LocalDate end, double credits) {
            when(repository.existsByEmployee_IdAndStartDateAndEndDate(empId, start, end)).thenReturn(false);
            when(repository.existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                    eq(empId), any(), eq(end), eq(start))).thenReturn(false);
            LeaveCredit credit = LeaveCredit.builder().credits(credits).build();
            when(leaveCreditService.getLeaveCreditByEmployeeIdAndType(empId, LeaveType.VACATION)).thenReturn(credit);
            when(repository.save(any(LeaveRequest.class))).thenAnswer(i -> i.getArgument(0));
        }

        @Test
        void shouldCountFiveWeekdaysForAFullMonToFriRange() {
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            stubValidationAndCreditCheck(1L, MON, FRI, 10.0);

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(FRI)
                    .build();

            // 10 credits - 5 days = 5 remaining; no exception means 5 days were accepted
            LeaveRequest result = service.createLeaveRequest(dto);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldCountOneWeekdayWhenStartAndEndAreTheSameWeekday() {
            LocalDate wednesday = LocalDate.of(2026, 3, 11);
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            stubValidationAndCreditCheck(1L, wednesday, wednesday, 1.0);

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(wednesday)
                    .endDate(wednesday)
                    .build();

            LeaveRequest result = service.createLeaveRequest(dto);
            assertThat(result).isNotNull();
        }

        @Test
        void shouldExcludeSaturdayAndSundayFromDayCount() {
            // Mon(9)–next Mon(16) = 8 calendar days → 6 weekdays (Mon 9, Tue 10, Wed 11, Thu 12, Fri 13, Mon 16)
            LocalDate nextMonday = LocalDate.of(2026, 3, 16);
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            stubValidationAndCreditCheck(1L, MON, nextMonday, 10.0);

            LeaveRequestDto dto = LeaveRequestDto.builder()
                    .leaveType("VACATION")
                    .startDate(MON)
                    .endDate(nextMonday)
                    .build();

            // 10 credits available, 6 weekdays required → should succeed (no 400)
            LeaveRequest result = service.createLeaveRequest(dto);
            assertThat(result).isNotNull();
        }
    }
}
