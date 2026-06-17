package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.RequestStatus;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OvertimeRequestServiceTest {

    @InjectMocks
    private OvertimeRequestService service;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserService userService;

    @Mock
    private OvertimeRequestRepository repository;

    private User hrUser;
    private User regularUser;
    private Employee otherEmployee;

    private static final LocalDate DATE = LocalDate.of(2026, 3, 1);

    @BeforeEach
    void setUp() {
        Role hrRole = new Role("HR");
        Role employeeRole = new Role("EMPLOYEE");

        Employee supervisor = Employee.builder().id(99L).build();

        Employee employee = Employee.builder().id(1L).supervisor(supervisor).build();
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

    @Nested
    class CreateOvertimeRequestTests {

        @Test
        void shouldCreateOvertimeRequestForSelfWhenNoEmployeeIdProvided() {
            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(DATE)
                    .overtimeHours(new BigDecimal("2.5"))
                    .reason("Late project deadline")
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndDate(2L, DATE)).thenReturn(false);
            when(employeeService.getEmployeeById(2L)).thenReturn(otherEmployee);
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.createOvertimeRequest(request);

            assertThat(result.getEmployee()).isEqualTo(otherEmployee);
            assertThat(result.getDate()).isEqualTo(DATE);
            assertThat(result.getOvertimeHours()).isEqualByComparingTo(new BigDecimal("2.5"));
            assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(result.getReason()).isEqualTo("Late project deadline");
        }

        @Test
        void shouldCreateOvertimeRequestOnBehalfOfAnotherEmployeeWhenRequesterIsHR() {
            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .employeeId(2L)
                    .date(DATE)
                    .reason("Project overtime")
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.existsByEmployee_IdAndDate(2L, DATE)).thenReturn(false);
            when(employeeService.getEmployeeById(2L)).thenReturn(otherEmployee);
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.createOvertimeRequest(request);

            assertThat(result.getEmployee()).isEqualTo(otherEmployee);
            assertThat(result.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        void shouldThrow403WhenNonHRUserAttemptsToCreateRequestForAnotherEmployee() {
            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .employeeId(1L)
                    .date(DATE)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);

            assertThatThrownBy(() -> service.createOvertimeRequest(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow409WhenOvertimeRequestAlreadyExistsForEmployeeAndDate() {
            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(DATE)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.existsByEmployee_IdAndDate(2L, DATE)).thenReturn(true);

            assertThatThrownBy(() -> service.createOvertimeRequest(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

    }

    @Nested
    class GetOvertimeRequestsTests {

        @Test
        void shouldReturnAllRequestsWhenNoDatesProvided() {
            Page<OvertimeRequest> page = new PageImpl<>(List.of());
            when(repository.findAll(any(Pageable.class))).thenReturn(page);

            Page<OvertimeRequest> result = service.getOvertimeRequests(null, null, 0, 10);

            assertThat(result).isEqualTo(page);
        }

        @Test
        void shouldReturnFilteredRequestsWhenDateRangeIsProvided() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);
            Page<OvertimeRequest> page = new PageImpl<>(List.of());
            when(repository.findByDateBetween(eq(start), eq(end), any(Pageable.class))).thenReturn(page);

            Page<OvertimeRequest> result = service.getOvertimeRequests(start, end, 0, 10);

            assertThat(result).isEqualTo(page);
        }

        @Test
        void shouldApplyDateRangeFilterWhenOnlyStartDateIsProvided() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            Page<OvertimeRequest> page = new PageImpl<>(List.of());
            when(repository.findByDateBetween(eq(start), any(LocalDate.class), any(Pageable.class))).thenReturn(page);

            Page<OvertimeRequest> result = service.getOvertimeRequests(start, null, 0, 10);

            assertThat(result).isEqualTo(page);
            verify(repository, never()).findAll(any(Pageable.class));
        }

    }

    @Nested
    class GetEmployeeOvertimeRequestTests {

        @Test
        void shouldReturnPagedRequestsBelongingToAuthenticatedEmployee() {
            Page<OvertimeRequest> page = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findAllByEmployee_Id(eq(2L), any(Pageable.class))).thenReturn(page);

            Page<OvertimeRequest> result = service.getEmployeeOvertimeRequest(0, 10);

            assertThat(result).isEqualTo(page);
        }
    }

    @Nested
    class GetSubordinatesOvertimeRequestsTests {

        @Test
        void shouldReturnPagedRequestsForAllSubordinatesOfAuthenticatedSupervisor() {
            Page<OvertimeRequest> page = new PageImpl<>(List.of());
            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findByEmployee_Supervisor_Id(eq(1L), any(Pageable.class))).thenReturn(page);

            Page<OvertimeRequest> result = service.getSubordinatesOvertimeRequests(0, 10);

            assertThat(result).isEqualTo(page);
        }
    }

    @Nested
    class GetOvertimeRequestByIdTests {

        @Test
        void shouldReturnRequestWhenOwnerAccessesIt() {
            UUID id = UUID.randomUUID();
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            OvertimeRequest result = service.getOvertimeRequestById(id);

            assertThat(result).isEqualTo(request);
        }

        @Test
        void shouldReturnRequestWhenHRAccessesIt() {
            UUID id = UUID.randomUUID();
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            OvertimeRequest result = service.getOvertimeRequestById(id);

            assertThat(result).isEqualTo(request);
        }

        @Test
        void shouldReturnRequestWhenDirectSupervisorAccessesIt() {
            UUID id = UUID.randomUUID();
            // employee's supervisor is employee (id=1L), hrUser is associated with employee (id=1L)
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            OvertimeRequest result = service.getOvertimeRequestById(id);

            assertThat(result).isEqualTo(request);
        }

        @Test
        void shouldThrow404WhenOvertimeRequestDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getOvertimeRequestById(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        void shouldThrow403WhenRequesterIsNeitherOwnerNorHRNorSupervisor() {
            UUID id = UUID.randomUUID();

            Employee unrelatedEmployee = Employee.builder().id(3L).supervisor(null).build();
            Role role = new Role("EMPLOYEE");
            User unrelatedUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(unrelatedEmployee)
                    .role(role)
                    .build();

            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(unrelatedUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.getOvertimeRequestById(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class UpdateOvertimeRequestTests {

        @Test
        void shouldUpdateReasonWithoutChangingOvertimeHoursWhenDateIsUnchanged() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .date(DATE)
                    .overtimeHours(new BigDecimal("2.0"))
                    .status(RequestStatus.PENDING)
                    .reason("Old reason")
                    .build();

            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(DATE)
                    .reason("New reason")
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.updateOvertimeRequest(id, request);

            assertThat(result.getReason()).isEqualTo("New reason");
            assertThat(result.getOvertimeHours()).isEqualByComparingTo(new BigDecimal("2.0"));
            assertThat(result.getDate()).isEqualTo(DATE);
        }

        @Test
        void shouldUpdateDateWhenDateChanges() {
            UUID id = UUID.randomUUID();
            LocalDate newDate = DATE.plusDays(1);

            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .date(DATE)
                    .overtimeHours(new BigDecimal("1.0"))
                    .status(RequestStatus.PENDING)
                    .build();

            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(newDate)
                    .reason("Updated reason")
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.existsByEmployee_IdAndDate(2L, newDate)).thenReturn(false);
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.updateOvertimeRequest(id, request);

            assertThat(result.getDate()).isEqualTo(newDate);
        }

        @Test
        void shouldThrow400WhenAttemptingToUpdateNonPendingRequest() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .date(DATE)
                    .status(RequestStatus.APPROVED)
                    .build();

            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(DATE)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateOvertimeRequest(id, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow409WhenNewDateAlreadyHasAnOvertimeRequestForSameEmployee() {
            UUID id = UUID.randomUUID();
            LocalDate newDate = DATE.plusDays(1);

            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .date(DATE)
                    .status(RequestStatus.PENDING)
                    .build();

            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(newDate)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.existsByEmployee_IdAndDate(2L, newDate)).thenReturn(true);

            assertThatThrownBy(() -> service.updateOvertimeRequest(id, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldThrow403WhenNonOwnerAttemptsToUpdateRequest() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .date(DATE)
                    .status(RequestStatus.PENDING)
                    .build();

            OvertimeRequestDto request = OvertimeRequestDto.builder()
                    .date(DATE)
                    .reason("Updated reason")
                    .build();

            Employee unrelatedEmployee = Employee.builder().id(3L).supervisor(null).build();
            Role role = new Role("EMPLOYEE");
            User unrelatedUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(unrelatedEmployee)
                    .role(role)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(unrelatedUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.updateOvertimeRequest(id, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class UpdateOvertimeRequestStatusTests {

        @Test
        void shouldApproveRequestWhenRequesterIsHR() {
            UUID id = UUID.randomUUID();
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.updateOvertimeRequestStatus(id, RequestStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        }

        @Test
        void shouldRejectRequestWhenRequesterIsHR() {
            UUID id = UUID.randomUUID();
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.updateOvertimeRequestStatus(id, RequestStatus.REJECTED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.REJECTED);
        }

        @Test
        void shouldApproveRequestWhenRequesterIsDirectSupervisor() {
            UUID id = UUID.randomUUID();
            // hrUser's employee (id=1L) is otherEmployee's supervisor (id=99L... but in setUp otherEmployee's supervisor is Employee id=99L)
            // Re-wire: make otherEmployee's supervisor be employee (id=1L) which hrUser owns
            Employee supervisorEmployee = Employee.builder().id(1L).build();
            Employee subordinate = Employee.builder().id(5L).supervisor(supervisorEmployee).build();

            Role supervisorRole = new Role("SUPERVISOR");
            User supervisorUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(supervisorEmployee)
                    .role(supervisorRole)
                    .build();

            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(subordinate)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(supervisorUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            OvertimeRequest result = service.updateOvertimeRequestStatus(id, RequestStatus.APPROVED);

            assertThat(result.getStatus()).isEqualTo(RequestStatus.APPROVED);
        }

        @Test
        void shouldThrow403WhenRequesterIsNeitherHRNorDirectSupervisor() {
            UUID id = UUID.randomUUID();

            Employee unrelatedEmployee = Employee.builder().id(3L).supervisor(null).build();
            Role role = new Role("EMPLOYEE");
            User unrelatedUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(unrelatedEmployee)
                    .role(role)
                    .build();

            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(unrelatedUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.updateOvertimeRequestStatus(id, RequestStatus.APPROVED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        void shouldThrow400WhenRequestHasAlreadyBeenProcessed() {
            UUID id = UUID.randomUUID();
            OvertimeRequest request = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.APPROVED)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(hrUser);
            when(repository.findById(id)).thenReturn(Optional.of(request));

            assertThatThrownBy(() -> service.updateOvertimeRequestStatus(id, RequestStatus.REJECTED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    class DeleteOvertimeRequestTests {

        @Test
        void shouldSoftDeleteRequestBySettingDeletedAtWhenStatusIsPending() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.save(any(OvertimeRequest.class))).thenAnswer(i -> i.getArgument(0));

            service.deleteOvertimeRequest(id);

            assertThat(existing.getDeletedAt()).isNotNull();
        }

        @Test
        void shouldThrow400WhenAttemptingToDeleteApprovedRequest() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.APPROVED)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteOvertimeRequest(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow400WhenAttemptingToDeleteRejectedRequest() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.REJECTED)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(regularUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteOvertimeRequest(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        void shouldThrow403WhenNonOwnerAttemptsToDeleteRequest() {
            UUID id = UUID.randomUUID();
            OvertimeRequest existing = OvertimeRequest.builder()
                    .id(id)
                    .employee(otherEmployee)
                    .status(RequestStatus.PENDING)
                    .build();

            Employee unrelatedEmployee = Employee.builder().id(3L).supervisor(null).build();
            Role role = new Role("EMPLOYEE");
            User unrelatedUser = User.builder()
                    .id(UUID.randomUUID())
                    .employee(unrelatedEmployee)
                    .role(role)
                    .build();

            when(userService.getAuthenticatedUser()).thenReturn(unrelatedUser);
            when(repository.findById(id)).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> service.deleteOvertimeRequest(id))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class CalculateApprovedOvertimeHoursTests {

        @Test
        void shouldReturnSumOfApprovedOvertimeHoursForEmployeeAndDateRange() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);

            when(repository.sumOvertimeHoursByEmployeeI_IdAndDateBetweenAndStatus(
                    1L, start, end, RequestStatus.APPROVED))
                    .thenReturn(new BigDecimal("12.5"));

            BigDecimal result = service.calculateApprovedOvertimeHours(1L, start, end);

            assertThat(result).isEqualByComparingTo(new BigDecimal("12.5"));
        }

        @Test
        void shouldReturnZeroWhenNoApprovedRequestsExistInRange() {
            LocalDate start = LocalDate.of(2026, 3, 1);
            LocalDate end = LocalDate.of(2026, 3, 31);

            when(repository.sumOvertimeHoursByEmployeeI_IdAndDateBetweenAndStatus(
                    1L, start, end, RequestStatus.APPROVED))
                    .thenReturn(BigDecimal.ZERO);

            BigDecimal result = service.calculateApprovedOvertimeHours(1L, start, end);

            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}

