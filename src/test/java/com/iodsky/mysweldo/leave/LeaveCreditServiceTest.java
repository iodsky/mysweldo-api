package com.iodsky.mysweldo.leave;

import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.leave.credit.LeaveCredit;
import com.iodsky.mysweldo.leave.credit.LeaveCreditRepository;
import com.iodsky.mysweldo.leave.credit.LeaveCreditRequest;
import com.iodsky.mysweldo.leave.credit.LeaveCreditService;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCreditServiceTest {

    @Mock private LeaveCreditRepository leaveCreditRepository;
    @Mock private EmployeeService employeeService;
    @Mock private UserService userService;
    @InjectMocks private LeaveCreditService leaveCreditService;

    private User normalUser;
    private Employee employee;
    private LeaveCredit vacationCredit;
    private LeaveCredit sickCredit;

    @BeforeEach
    void setUp() {
        // Setup employee
        employee = new Employee();
        employee.setId(1L);
        employee.setFirstName("Juan");
        employee.setLastName("Dela Cruz");

        // Setup user
        normalUser = new User();
        normalUser.setId(UUID.randomUUID());
        normalUser.setEmployee(employee);
        normalUser.setRole(new Role("EMPLOYEE"));

        // Setup leave credits
        vacationCredit = LeaveCredit.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .type(LeaveType.VACATION)
                .credits(10.0)
                .build();

        sickCredit = LeaveCredit.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .type(LeaveType.SICK)
                .credits(5.0)
                .build();
    }


    @Nested
    class CreateLeaveCreditsTests {

        @Test
        void shouldCreateLeaveCreditsSuccessfully() {
            java.time.LocalDate effectiveDate = LocalDate.of(2026, 1, 1);
            LeaveCreditRequest request = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(eq(1L))).thenReturn(employee);
            when(leaveCreditRepository.existsByEmployee_IdAndEffectiveDate(eq(1L), eq(effectiveDate)))
                    .thenReturn(false);

            LeaveCredit vacation = LeaveCredit.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .effectiveDate(effectiveDate)
                    .build();

            LeaveCredit sick = LeaveCredit.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .type(LeaveType.SICK)
                    .credits(7.0)
                    .effectiveDate(effectiveDate)
                    .build();

            LeaveCredit bereavement = LeaveCredit.builder()
                    .id(UUID.randomUUID())
                    .employee(employee)
                    .type(LeaveType.BEREAVEMENT)
                    .credits(5.0)
                    .effectiveDate(effectiveDate)
                    .build();

            when(leaveCreditRepository.saveAll(anyList()))
                    .thenReturn(List.of(vacation, sick, bereavement));

            List<LeaveCredit> result = leaveCreditService.createLeaveCredits(request);

            assertNotNull(result);
            assertEquals(3, result.size());

            // Verify vacation credit
            LeaveCredit vacationResult = result.stream()
                    .filter(lc -> lc.getType() == LeaveType.VACATION)
                    .findFirst()
                    .orElse(null);
            assertNotNull(vacationResult);
            assertEquals(14.0, vacationResult.getCredits());
            assertEquals(effectiveDate, vacationResult.getEffectiveDate());

            // Verify sick credit
            LeaveCredit sickResult = result.stream()
                    .filter(lc -> lc.getType() == LeaveType.SICK)
                    .findFirst()
                    .orElse(null);
            assertNotNull(sickResult);
            assertEquals(7.0, sickResult.getCredits());
            assertEquals(effectiveDate, sickResult.getEffectiveDate());

            // Verify bereavement credit
            LeaveCredit bereavementResult = result.stream()
                    .filter(lc -> lc.getType() == LeaveType.BEREAVEMENT)
                    .findFirst()
                    .orElse(null);
            assertNotNull(bereavementResult);
            assertEquals(5.0, bereavementResult.getCredits());
            assertEquals(effectiveDate, bereavementResult.getEffectiveDate());

            verify(employeeService).getEmployeeById(eq(1L));
            verify(leaveCreditRepository).existsByEmployee_IdAndEffectiveDate(eq(1L), eq(effectiveDate));
            verify(leaveCreditRepository).saveAll(anyList());
        }

        @Test
        void shouldThrowConflictWhenLeaveCreditsAlreadyExist() {
            LocalDate effectiveDate = LocalDate.of(2026, 1, 1);
            LeaveCreditRequest request = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(eq(1L))).thenReturn(employee);
            when(leaveCreditRepository.existsByEmployee_IdAndEffectiveDate(eq(1L), eq(effectiveDate)))
                    .thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    leaveCreditService.createLeaveCredits(request));

            assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("Leave credits already exists"));
            assertTrue(ex.getMessage().contains("employee 1"));

            verify(employeeService).getEmployeeById(eq(1L));
            verify(leaveCreditRepository).existsByEmployee_IdAndEffectiveDate(eq(1L), eq(effectiveDate));
            verify(leaveCreditRepository, never()).saveAll(anyList());
        }

        @Test
        void shouldThrowNotFoundWhenEmployeeDoesNotExist() {
            LocalDate effectiveDate = LocalDate.of(2026, 1, 1);
            LeaveCreditRequest request = LeaveCreditRequest.builder()
                    .employeeId(999L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(eq(999L)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: 999"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    leaveCreditService.createLeaveCredits(request));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("Employee not found"));

            verify(employeeService).getEmployeeById(eq(999L));
            verify(leaveCreditRepository, never()).existsByEmployee_IdAndEffectiveDate(anyLong(), any());
            verify(leaveCreditRepository, never()).saveAll(anyList());
        }

    }

    @Nested
    class GetLeaveCreditByEmployeeIdAndTypeTests {

        @Test
        void shouldReturnLeaveCreditSuccessfully() {
            when(leaveCreditRepository.findByEmployee_IdAndType(eq(1L), eq(LeaveType.VACATION)))
                    .thenReturn(Optional.of(vacationCredit));

            LeaveCredit result = leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION);

            assertNotNull(result);
            assertEquals(LeaveType.VACATION, result.getType());
            assertEquals(10.0, result.getCredits());
        }

        @Test
        void shouldThrowNotFoundWhenLeaveCreditDoesNotExist() {
            when(leaveCreditRepository.findByEmployee_IdAndType(eq(1L), eq(LeaveType.VACATION)))
                    .thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    leaveCreditService.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertTrue(ex.getMessage().contains("VACATION"));
            assertTrue(ex.getMessage().contains("employeeId: 1"));
        }
    }

    @Nested
    class GetLeaveCreditsByEmployeeIdTests {

        @Test
        void shouldReturnAllLeaveCreditsForAuthenticatedEmployee() {
            when(userService.getAuthenticatedUser()).thenReturn(normalUser);
            when(leaveCreditRepository.findAllByEmployee_Id(eq(1L)))
                    .thenReturn(List.of(vacationCredit, sickCredit));

            List<LeaveCredit> result = leaveCreditService.getLeaveCreditsByEmployeeId();

            assertEquals(2, result.size());
            verify(userService).getAuthenticatedUser();
            verify(leaveCreditRepository).findAllByEmployee_Id(eq(1L));
        }

        @Test
        void shouldThrowUnauthorizedWhenPrincipalIsNotUser() {
            when(userService.getAuthenticatedUser())
                    .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    leaveCreditService.getLeaveCreditsByEmployeeId());

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            verify(userService).getAuthenticatedUser();
        }
    }

    @Nested
    class UpdateLeaveCreditTests {

        @Test
        void shouldUpdateLeaveCreditSuccessfully() {
            UUID creditId = vacationCredit.getId();
            LeaveCredit updatedCredit = LeaveCredit.builder()
                    .id(creditId)
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(8.0)
                    .build();

            when(leaveCreditRepository.findById(creditId))
                    .thenReturn(Optional.of(vacationCredit));
            when(leaveCreditRepository.save(any(LeaveCredit.class)))
                    .thenReturn(updatedCredit);

            LeaveCredit result = leaveCreditService.updateLeaveCredit(creditId, updatedCredit);

            assertNotNull(result);
            assertEquals(8.0, result.getCredits());
            verify(leaveCreditRepository).save(vacationCredit);
        }

        @Test
        void shouldThrowNotFoundWhenLeaveCreditDoesNotExist() {
            UUID creditId = UUID.randomUUID();
            when(leaveCreditRepository.findById(creditId))
                    .thenReturn(Optional.empty());

            LeaveCredit updatedCredit = LeaveCredit.builder()
                    .credits(8.0)
                    .build();

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                    leaveCreditService.updateLeaveCredit(creditId, updatedCredit));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

}

