package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.benefit.BenefitService;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentService;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionService;
import com.iodsky.mysweldo.security.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService service;

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DepartmentService departmentService;

    @Mock
    private PositionService positionService;

    @Mock
        private BenefitService benefitService;

    private Department department;
    private Position position;
    private Employee savedEmployee;

    @BeforeEach
    void setUp() {
        department = Department.builder().id("DEPT-001").title("Engineering").build();
        position = Position.builder().id("POS-001").title("Engineer").build();
        savedEmployee = Employee.builder().id(1L).firstName("John").lastName("Doe").build();
        savedEmployee.setBenefits(new ArrayList<>());
    }

    @Nested
    class CreateEmployeeTests {

        @Test
        void shouldCreateEmployeeWithSupervisorWhenSupervisorIdProvided() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(2L);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            Employee supervisor = Employee.builder().id(2L).build();
            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>());

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(employeeRepository.findById(2L)).thenReturn(Optional.of(supervisor));
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(employeeRepository.save(mappedEmployee)).thenReturn(savedEmployee);

            Employee result = service.createEmployee(request);

            assertThat(result).isNotNull();
            assertThat(mappedEmployee.getSupervisor()).isEqualTo(supervisor);
            assertThat(mappedEmployee.getDepartment()).isEqualTo(department);
            assertThat(mappedEmployee.getPosition()).isEqualTo(position);
            verify(employeeRepository).save(mappedEmployee);
        }

        @Test
        void shouldCreateEmployeeWithNullSupervisorWhenSupervisorIdIsNull() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>());

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(employeeRepository.save(mappedEmployee)).thenReturn(savedEmployee);

            Employee result = service.createEmployee(request);

            assertThat(result).isNotNull();
            assertThat(mappedEmployee.getSupervisor()).isNull();
            verify(employeeRepository, never()).findById(any());
        }

        @Test
        void shouldResolveEachBenefitTypeWhenBenefitsArePresent() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            Benefit benefitType = Benefit.builder().code("BT-001").build();
            EmployeeBenefit benefit = new EmployeeBenefit();
            benefit.setBenefit(benefitType);

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>(List.of(benefit)));

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(benefitService.getBenefitByCode("BT-001")).thenReturn(benefitType);
            when(employeeRepository.save(mappedEmployee)).thenReturn(savedEmployee);

            Employee result = service.createEmployee(request);

            assertThat(result).isNotNull();
            verify(benefitService).getBenefitByCode("BT-001");
        }

        @Test
        void shouldPropagateExceptionWhenDepartmentNotFound() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-MISSING");

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>());

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(departmentService.getDepartmentById("DEPT-MISSING"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));

            // Act & Assert
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Department not found");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenPositionNotFound() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-MISSING");

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>());

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-MISSING"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));

            // Act & Assert
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Position not found");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenSupervisorNotFound() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(99L);

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>());

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenBenefitTypeNotFound() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            Benefit unknownType = Benefit.builder().code("BT-MISSING").build();
            EmployeeBenefit benefit = new EmployeeBenefit();
            benefit.setBenefit(unknownType);

            Employee mappedEmployee = Employee.builder().id(1L).build();
            mappedEmployee.setBenefits(new ArrayList<>(List.of(benefit)));

            when(employeeMapper.toEntity(request)).thenReturn(mappedEmployee);
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(benefitService.getBenefitByCode("BT-MISSING"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Benefit type not found"));

            // Act & Assert
            assertThatThrownBy(() -> service.createEmployee(request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Benefit type not found");

            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    class GetAllEmployeesTests {

        @Test
        void shouldReturnEmployeesFilteredByDepartmentIdWhenDepartmentIdIsProvided() {
            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .departmentId("DEPT-01")
                    .departmentTitle("DEVELOPMENT")
                    .positionId("SD-01")
                    .positionTitle("SOFTWARE DEVELOPER")
                    .status(EmploymentStatus.REGULAR)
                    .type(EmploymentType.FULL_TIME)
                    .build();

            Page<EmployeeBasic> expected = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllByDepartment_Id(eq("DEPT-01"), any(Pageable.class)))
                    .thenReturn(expected);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, "DEPT-01", null, null);

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository)
                    .findAllByDepartment_Id(eq("DEPT-01"), any(Pageable.class));
        }

        @Test
        void shouldReturnEmployeesFilteredBySupervisorIdWhenSupervisorIdIsProvidedAndDepartmentIdIsNull() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            Page<EmployeeBasic> expected = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllBySupervisor_Id(eq(2L), any(Pageable.class)))
                    .thenReturn(expected);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, null, 2L, null);

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository).findAllBySupervisor_Id(eq(2L), any(Pageable.class));
        }

        @Test
        void shouldReturnEmployeesFilteredByStatusWhenStatusIsProvidedAndOtherFiltersAreNull() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .status(EmploymentStatus.REGULAR)
                    .build();

            Page<EmployeeBasic> repoResult = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllByStatus(eq(EmploymentStatus.REGULAR), any(Pageable.class)))
                    .thenReturn(repoResult);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, null, null, "REGULAR");

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository)
                    .findAllByStatus(eq(EmploymentStatus.REGULAR), any(Pageable.class));
        }

        @Test
        void shouldReturnAllEmployeesWhenNoFiltersAreProvided() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            Page<EmployeeBasic> repoResult = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllByStatusNotIn(anyList(), any(Pageable.class)))
                    .thenReturn(repoResult);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, null, null, null);

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository).findAllByStatusNotIn(anyList(), any(Pageable.class));
        }

        @Test
        void shouldFilterByDepartmentIdWhenBothDepartmentIdAndSupervisorIdAreProvided() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .departmentId("DEPT-01")
                    .build();

            Page<EmployeeBasic> repoResult = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllByDepartment_Id(eq("DEPT-01"), any(Pageable.class)))
                    .thenReturn(repoResult);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, "DEPT-01", 2L, null);

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository)
                    .findAllByDepartment_Id(eq("DEPT-01"), any(Pageable.class));

            verify(employeeRepository, never())
                    .findAllBySupervisor_Id(any(), any(Pageable.class));
        }

        @Test
        void shouldFilterBySupervisorIdWhenBothSupervisorIdAndStatusAreProvidedAndDepartmentIdIsNull() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .build();

            Page<EmployeeBasic> repoResult = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllBySupervisor_Id(eq(2L), any(Pageable.class)))
                    .thenReturn(repoResult);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, null, 2L, "REGULAR");

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository)
                    .findAllBySupervisor_Id(eq(2L), any(Pageable.class));

            verify(employeeRepository, never())
                    .findAllByStatus(any(), any(Pageable.class));
        }

        @Test
        void shouldResolveStatusCaseInsensitivelyWhenLowercaseStatusIsProvided() {

            EmployeeBasic projection = EmployeeBasicStub.builder()
                    .id(1L)
                    .firstName("John")
                    .lastName("Doe")
                    .status(EmploymentStatus.REGULAR)
                    .build();

            Page<EmployeeBasic> repoResult = new PageImpl<>(List.of(projection));

            when(employeeRepository.findAllByStatus(eq(EmploymentStatus.REGULAR), any(Pageable.class)))
                    .thenReturn(repoResult);

            Page<EmployeeBasicDto> result =
                    service.getAllEmployees(0, 10, null, null, "regular");

            assertThat(result.getContent()).hasSize(1);

            verify(employeeRepository)
                    .findAllByStatus(eq(EmploymentStatus.REGULAR), any(Pageable.class));
        }

        @Test
        void shouldThrowIllegalArgumentExceptionWhenInvalidStatusStringIsProvided() {
            // Act & Assert
            assertThatThrownBy(() -> service.getAllEmployees(0, 10, null, null, "INVALID_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class GetAuthenticatedEmployeeTests {

        @Test
        void shouldReturnEmployeeWhenPrincipalIsUser() {
            Employee employee = Employee.builder().id(1L).build();
            User user = mock(User.class);
            when(user.getEmployee()).thenReturn(employee);

            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn(user);

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                    Employee result = service.getAuthenticatedEmployee();

                    assertThat(result).isEqualTo(employee);
            }
        }

        @Test
        void shouldThrow401WhenPrincipalIsNotAUser() {
            Authentication authentication = mock(Authentication.class);
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(authentication);

            try (MockedStatic<SecurityContextHolder> holder = mockStatic(SecurityContextHolder.class)) {
                holder.when(SecurityContextHolder::getContext).thenReturn(securityContext);

                // Act & Assert
                assertThatThrownBy(() -> service.getAuthenticatedEmployee())
                        .isInstanceOf(ResponseStatusException.class)
                        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    @Nested
    class GetEmployeeByIdTests {

        @Test
        void shouldReturnEmployeeWhenEmployeeExists() {
            when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));

            Employee result = service.getEmployeeById(1L);

            assertThat(result).isEqualTo(savedEmployee);
        }

        @Test
        void shouldThrow404WithEmployeeIdInMessageWhenEmployeeNotFound() {
            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.getEmployeeById(999L))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                        assertThat(rse.getReason()).contains("999");
                    });
        }
    }

    @Nested
    class UpdateEmployeeByIdTests {

        @Test
        void shouldUpdateAndSaveEmployeeWithSupervisorWhenSupervisorIdIsProvided() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(2L);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            Employee supervisor = Employee.builder().id(2L).build();

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));
            when(employeeRepository.findById(2L)).thenReturn(Optional.of(supervisor));
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(employeeRepository.save(savedEmployee)).thenReturn(savedEmployee);

            Employee result = service.updateEmployeeById(1L, request);

            assertThat(result).isEqualTo(savedEmployee);
            assertThat(savedEmployee.getSupervisor()).isEqualTo(supervisor);
            assertThat(savedEmployee.getDepartment()).isEqualTo(department);
            assertThat(savedEmployee.getPosition()).isEqualTo(position);
            verify(employeeRepository).save(savedEmployee);
        }

        @Test
        void shouldSetSupervisorToNullWhenSupervisorIdIsNullInRequest() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-001");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-001")).thenReturn(position);
            when(employeeRepository.save(savedEmployee)).thenReturn(savedEmployee);

            service.updateEmployeeById(1L, request);

            assertThat(savedEmployee.getSupervisor()).isNull();
        }

        @Test
        void shouldThrow404WhenEmployeeToUpdateNotFound() {
            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.updateEmployeeById(999L, mock(EmployeeRequest.class)))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenDepartmentNotFoundDuringUpdate() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-MISSING");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));
            when(departmentService.getDepartmentById("DEPT-MISSING"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found"));

            // Act & Assert
            assertThatThrownBy(() -> service.updateEmployeeById(1L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Department not found");

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldPropagateExceptionWhenPositionNotFoundDuringUpdate() {
            EmployeeRequest request = mock(EmployeeRequest.class);
            when(request.getSupervisorId()).thenReturn(null);
            when(request.getDepartmentId()).thenReturn("DEPT-001");
            when(request.getPositionId()).thenReturn("POS-MISSING");

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(savedEmployee));
            when(departmentService.getDepartmentById("DEPT-001")).thenReturn(department);
            when(positionService.getPositionById("POS-MISSING"))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Position not found"));

            // Act & Assert
            assertThatThrownBy(() -> service.updateEmployeeById(1L, request))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("Position not found");

            verify(employeeRepository, never()).save(any());
        }
    }

    @Nested
    class UpdateEmployeeStatusTests {

        @Test
        void shouldTerminateEmployeeAndUnlinkSubordinatesWhenStatusIsTerminated() {
            Employee employee = Employee.builder().id(1L).build();
            Employee subordinate = Employee.builder().id(2L).build();
            subordinate.setSupervisor(employee);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(employeeRepository.findAllBySupervisor_Id(1L)).thenReturn(List.of(subordinate));

            service.updateEmployeeStatus(1L, EmploymentStatus.TERMINATED);

            assertThat(employee.getStatus()).isEqualTo(EmploymentStatus.TERMINATED);
            assertThat(employee.getDeletedAt()).isNull();
            assertThat(subordinate.getSupervisor()).isNull();
            verify(employeeRepository).save(employee);
        }

        @Test
        void shouldResignEmployeeWhenStatusIsResigned() {
            Employee employee = Employee.builder().id(1L).build();

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
            when(employeeRepository.findAllBySupervisor_Id(1L)).thenReturn(Collections.emptyList());

            service.updateEmployeeStatus(1L, EmploymentStatus.RESIGNED);

            assertThat(employee.getStatus()).isEqualTo(EmploymentStatus.RESIGNED);
            verify(employeeRepository).save(employee);
        }

        @Test
        void shouldThrow400WhenEmployeeIsAlreadyTerminated() {
            Employee employee = Employee.builder().id(1L).build();
            employee.setStatus(EmploymentStatus.TERMINATED);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> service.updateEmployeeStatus(1L, EmploymentStatus.TERMINATED))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldThrow400WhenEmployeeIsAlreadyResigned() {
            Employee employee = Employee.builder().id(1L).build();
            employee.setStatus(EmploymentStatus.RESIGNED);

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> service.updateEmployeeStatus(1L, EmploymentStatus.TERMINATED))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    });

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldThrow400WhenFinalStatusIsNotTerminatedOrResigned() {
            Employee employee = Employee.builder().id(1L).build();

            when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

            // Act & Assert
            assertThatThrownBy(() -> service.updateEmployeeStatus(1L, EmploymentStatus.PROBATIONARY))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(rse.getReason()).isEqualTo("Final status must be TERMINATED or RESIGNED");
                    });

            verify(employeeRepository, never()).save(any());
        }

        @Test
        void shouldThrow404WhenEmployeeToDeleteNotFound() {
            when(employeeRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.updateEmployeeStatus(999L, EmploymentStatus.TERMINATED))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class GetAllActiveEmployeeIdsTests {

        @Test
        void shouldReturnListOfActiveEmployeeIds() {
            List<Long> expectedIds = List.of(1L, 2L, 3L);
            when(employeeRepository.findAllActiveEmployeeIds()).thenReturn(expectedIds);

            List<Long> result = service.getAllActiveEmployeeIds();

            assertThat(result).containsExactlyElementsOf(expectedIds);
        }

        @Test
        void shouldReturnEmptyListWhenNoActiveEmployeesExist() {
            when(employeeRepository.findAllActiveEmployeeIds()).thenReturn(Collections.emptyList());

            List<Long> result = service.getAllActiveEmployeeIds();

            assertThat(result).isNotNull().isEmpty();
        }
    }
}

