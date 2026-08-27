package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeBasic;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.leave.LeaveType;
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
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveCreditServiceTest {

    @InjectMocks
    private LeaveCreditService service;

    @Mock
    private LeaveCreditRepository repository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private UserService userService;

    private Employee employee;
    private LocalDate effectiveDate;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(1L).build();
        effectiveDate = LocalDate.of(2025, 1, 1);
    }

    @Nested
    class CreateLeaveCreditsTests {

        @Test
        void shouldCreateThreeLeaveCreditsWhenNoneExistForEffectiveDate() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(false);
            when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<LeaveCredit> result = service.createLeaveCredits(dto);

            assertThat(result).hasSize(3);
            assertThat(result).extracting(LeaveCredit::getType)
                    .containsExactlyInAnyOrder(LeaveType.VACATION, LeaveType.SICK, LeaveType.BEREAVEMENT);
        }

        @Test
        void shouldAssignDefaultCreditValuesForEachLeaveType() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(false);
            when(repository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<LeaveCredit> result = service.createLeaveCredits(dto);

            LeaveCredit vacation = result.stream().filter(c -> c.getType() == LeaveType.VACATION).findFirst().orElseThrow();
            LeaveCredit sick = result.stream().filter(c -> c.getType() == LeaveType.SICK).findFirst().orElseThrow();
            LeaveCredit bereavement = result.stream().filter(c -> c.getType() == LeaveType.BEREAVEMENT).findFirst().orElseThrow();

            assertThat(vacation.getCredits()).isEqualTo(14.0);
            assertThat(sick.getCredits()).isEqualTo(7.0);
            assertThat(bereavement.getCredits()).isEqualTo(5.0);
        }

        @Test
        void shouldThrow409WhenLeaveCreditsAlreadyExistForEffectiveDate() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(1L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(repository.existsByEmployee_IdAndEffectiveDate(1L, effectiveDate)).thenReturn(true);

            assertThatThrownBy(() -> service.createLeaveCredits(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        void shouldPropagateExceptionWhenEmployeeDoesNotExist() {
            LeaveCreditRequest dto = LeaveCreditRequest.builder()
                    .employeeId(99L)
                    .effectiveDate(effectiveDate)
                    .build();

            when(employeeService.getEmployeeById(99L))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee 99 not found"));

            assertThatThrownBy(() -> service.createLeaveCredits(dto))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetAllLeaveCreditsTests {

        @Test
        void shouldReturnPaginatedEmployeeLeaveCreditDtos() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp1 = mock(EmployeeBasic.class);
            when(emp1.getId()).thenReturn(1L);
            when(emp1.getFirstName()).thenReturn("John");
            when(emp1.getLastName()).thenReturn("Doe");

            EmployeeBasic emp2 = mock(EmployeeBasic.class);
            when(emp2.getId()).thenReturn(2L);
            when(emp2.getFirstName()).thenReturn("Jane");
            when(emp2.getLastName()).thenReturn("Smith");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp1, emp2),
                    PageRequest.of(pageNo, limit),
                    2
            );

            Employee empEntity1 = Employee.builder().id(1L).build();
            Employee empEntity2 = Employee.builder().id(2L).build();

            LeaveCredit vacation1 = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .employee(empEntity1)
                    .build();
            LeaveCredit sick1 = LeaveCredit.builder()
                    .type(LeaveType.SICK)
                    .credits(7.0)
                    .employee(empEntity1)
                    .build();
            LeaveCredit vacation2 = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(12.0)
                    .employee(empEntity2)
                    .build();

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L, 2L)))
                    .thenReturn(List.of(vacation1, sick1, vacation2));

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @Test
        void shouldMapEmployeeInformationCorrectly() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    1
            );

            Employee empEntity = Employee.builder().id(1L).build();
            LeaveCredit vacation = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .employee(empEntity)
                    .build();

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of(vacation));

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            EmployeeLeaveCreditDto dto = result.getContent().getFirst();
            assertThat(dto.getEmployeeId()).isEqualTo(1L);
            assertThat(dto.getFirstName()).isEqualTo("John");
            assertThat(dto.getLastName()).isEqualTo("Doe");
        }

        @Test
        void shouldGroupCreditsByEmployeeId() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    1
            );

            Employee empForLeaveCredit = Employee.builder().id(1L).build();
            LeaveCredit vacation = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .employee(empForLeaveCredit)
                    .build();
            LeaveCredit sick = LeaveCredit.builder()
                    .type(LeaveType.SICK)
                    .credits(7.0)
                    .employee(empForLeaveCredit)
                    .build();
            LeaveCredit bereavement = LeaveCredit.builder()
                    .type(LeaveType.BEREAVEMENT)
                    .credits(5.0)
                    .employee(empForLeaveCredit)
                    .build();

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of(vacation, sick, bereavement));

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            EmployeeLeaveCreditDto dto = result.getContent().getFirst();
            assertThat(dto.getCredits()).hasSize(3);
            assertThat(dto.getCredits()).extracting(CreditSummary::getType)
                    .containsExactlyInAnyOrder("VACATION", "SICK", "BEREAVEMENT");
        }

        @Test
        void shouldConvertLeaveTypeToCreditSummaryCorrectly() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    1
            );

            Employee empForLeaveCredit = Employee.builder().id(1L).build();
            LeaveCredit vacation = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .employee(empForLeaveCredit)
                    .build();

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of(vacation));

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            EmployeeLeaveCreditDto dto = result.getContent().getFirst();
            CreditSummary credit = dto.getCredits().getFirst();
            assertThat(credit.getType()).isEqualTo("VACATION");
            assertThat(credit.getCredits()).isEqualTo(14.0);
        }

        @Test
        void shouldReturnEmptyCreditsListWhenEmployeeHasNoCredits() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    1
            );

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of());

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            EmployeeLeaveCreditDto dto = result.getContent().getFirst();
            assertThat(dto.getCredits()).isEmpty();
        }

        @Test
        void shouldReturnEmptyPageWhenNoEmployeesExist() {
            int pageNo = 0;
            int limit = 10;

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(pageNo, limit),
                    0
            );

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        void shouldCallEmployeeServiceWithCorrectPageable() {
            int pageNo = 2;
            int limit = 20;

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(),
                    PageRequest.of(pageNo, limit),
                    0
            );

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);

            service.getAllLeaveCredits(pageNo, limit);

            verify(employeeService).getEmployees(argThat(pageable ->
                    pageable.getPageNumber() == pageNo && pageable.getPageSize() == limit
            ));
        }

        @Test
        void shouldCallRepositoryWithAllEmployeeIds() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp1 = mock(EmployeeBasic.class);
            when(emp1.getId()).thenReturn(1L);
            when(emp1.getFirstName()).thenReturn("John");
            when(emp1.getLastName()).thenReturn("Doe");

            EmployeeBasic emp2 = mock(EmployeeBasic.class);
            when(emp2.getId()).thenReturn(2L);
            when(emp2.getFirstName()).thenReturn("Jane");
            when(emp2.getLastName()).thenReturn("Smith");

            EmployeeBasic emp3 = mock(EmployeeBasic.class);
            when(emp3.getId()).thenReturn(3L);
            when(emp3.getFirstName()).thenReturn("Bob");
            when(emp3.getLastName()).thenReturn("Johnson");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp1, emp2, emp3),
                    PageRequest.of(pageNo, limit),
                    3
            );

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L, 2L, 3L)))
                    .thenReturn(List.of());

            service.getAllLeaveCredits(pageNo, limit);

            verify(repository).findAllByEmployee_IdIn(argThat(ids ->
                    ids.size() == 3 && ids.containsAll(List.of(1L, 2L, 3L))
            ));
        }

        @Test
        void shouldPreserveTotalElementsFromEmployeePage() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    100  // Total of 100 employees
            );

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of());

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            assertThat(result.getTotalElements()).isEqualTo(100);
        }

        @Test
        void shouldHandleMultipleCreditsPerEmployee() {
            int pageNo = 0;
            int limit = 10;

            EmployeeBasic emp = mock(EmployeeBasic.class);
            when(emp.getId()).thenReturn(1L);
            when(emp.getFirstName()).thenReturn("John");
            when(emp.getLastName()).thenReturn("Doe");

            Page<EmployeeBasic> employeePage = new PageImpl<>(
                    List.of(emp),
                    PageRequest.of(pageNo, limit),
                    1
            );

            Employee empForLeaveCredit = Employee.builder().id(1L).build();
            LeaveCredit vacation = LeaveCredit.builder()
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .employee(empForLeaveCredit)
                    .build();
            LeaveCredit sick = LeaveCredit.builder()
                    .type(LeaveType.SICK)
                    .credits(7.0)
                    .employee(empForLeaveCredit)
                    .build();
            LeaveCredit bereavement = LeaveCredit.builder()
                    .type(LeaveType.BEREAVEMENT)
                    .credits(5.0)
                    .employee(empForLeaveCredit)
                    .build();

            when(employeeService.getEmployees(any(Pageable.class))).thenReturn(employeePage);
            when(repository.findAllByEmployee_IdIn(List.of(1L)))
                    .thenReturn(List.of(vacation, sick, bereavement));

            Page<EmployeeLeaveCreditDto> result = service.getAllLeaveCredits(pageNo, limit);

            EmployeeLeaveCreditDto dto = result.getContent().getFirst();
            assertThat(dto.getCredits()).hasSize(3);
            assertThat(dto.getCredits())
                    .extracting(CreditSummary::getCredits)
                    .containsExactlyInAnyOrder(14.0, 7.0, 5.0);
        }
    }

    @Nested
    class GetLeaveCreditByEmployeeIdAndTypeTests {

        @Test
        void shouldReturnLeaveCreditWhenItExistsForEmployeeAndType() {
            LeaveCredit credit = LeaveCredit.builder()
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .effectiveDate(effectiveDate)
                    .build();

            when(repository.findByEmployee_IdAndType(1L, LeaveType.VACATION)).thenReturn(Optional.of(credit));

            LeaveCredit result = service.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.VACATION);

            assertThat(result).isEqualTo(credit);
        }

        @Test
        void shouldThrow404WhenNoCreditExistsForEmployeeAndType() {
            when(repository.findByEmployee_IdAndType(1L, LeaveType.SICK)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getLeaveCreditByEmployeeIdAndType(1L, LeaveType.SICK))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetLeaveCreditsByEmployeeIdTests {

        @Test
        void shouldReturnAllLeaveCreditsForAuthenticatedEmployee() {
            User user = mock(User.class);
            when(user.getEmployee()).thenReturn(employee);
            when(userService.getAuthenticatedUser()).thenReturn(user);

            List<LeaveCredit> credits = List.of(
                    LeaveCredit.builder().type(LeaveType.VACATION).credits(14.0).build(),
                    LeaveCredit.builder().type(LeaveType.SICK).credits(7.0).build()
            );
            when(repository.findAllByEmployee_Id(1L)).thenReturn(credits);

            List<LeaveCredit> result = service.getLeaveCreditsByEmployeeId();

            assertThat(result).isEqualTo(credits);
        }
    }

    @Nested
    class UpdateLeaveCreditTests {

        @Test
        void shouldUpdateAndReturnCreditWithNewValueWhenCreditExists() {
            UUID id = UUID.randomUUID();
            LeaveCredit existing = LeaveCredit.builder()
                    .employee(employee)
                    .type(LeaveType.VACATION)
                    .credits(14.0)
                    .effectiveDate(effectiveDate)
                    .build();
            LeaveCredit updated = LeaveCredit.builder().credits(10.0).build();

            when(repository.findById(id)).thenReturn(Optional.of(existing));
            when(repository.save(existing)).thenReturn(existing);

            LeaveCredit result = service.updateLeaveCredit(id, updated);

            assertThat(result.getCredits()).isEqualTo(10.0);
        }

        @Test
        void shouldThrow404WhenCreditToUpdateDoesNotExist() {
            UUID id = UUID.randomUUID();
            LeaveCredit updated = LeaveCredit.builder().credits(10.0).build();

            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLeaveCredit(id, updated))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}