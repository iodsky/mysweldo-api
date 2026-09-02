package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitRepository;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentRepository;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.employee.EmployeeRepository;
import com.iodsky.mysweldo.employee.PayType;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeImportServiceTest {

    @InjectMocks
    private EmployeeImportService service;

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportJobErrorRepository importJobErrorRepository;

    @Mock
    private PositionRepository positionRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private BenefitRepository benefitRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private Position engineerPosition;
    private Department engineering;

    @BeforeEach
    void setUp() {
        engineering = Department.builder().id("ENG").title("ENGINEERING").build();
        engineerPosition = Position.builder().id("ENGINEER").title("Engineer").department(engineering).build();

        lenient().when(positionRepository.findAll()).thenReturn(List.of(engineerPosition));
        lenient().when(departmentRepository.findAll()).thenReturn(List.of(engineering));
        lenient().when(benefitRepository.findAll()).thenReturn(List.of(
                Benefit.builder().code("MEAL").build(),
                Benefit.builder().code("PHONE").build(),
                Benefit.builder().code("CLOTHING").build()
        ));
    }

    @Test
    void importRecord_mapsCsvRowToEmployee() {
        EmployeeImportRecord record = EmployeeImportRecord.builder()
                .lastName("Doe")
                .firstName("John")
                .birthday("1990-01-15")
                .address("123 Main St")
                .phoneNumber("09171234567")
                .sssNumber("SSS-1")
                .tinNumber("TIN-1")
                .philhealthNumber("PHIL-1")
                .pagIbigNumber("PAGIBIG-1")
                .status("REGULAR")
                .employmentType("FULL_TIME")
                .position("Engineer")
                .startShift("09:00")
                .endShift("18:00")
                .rate("30000")
                .payType("MONTHLY")
                .payrollFrequency("SEMI_MONTHLY")
                .mealAllowance("1000")
                .phoneAllowance("500")
                .clothingAllowance("2000")
                .build();

        service.importRecord(record);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());

        Employee saved = captor.getValue();
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getPosition()).isSameAs(engineerPosition);
        assertThat(saved.getDepartment()).isSameAs(engineering);
        assertThat(saved.getSupervisor()).isNull();
        assertThat(saved.getSalary().getRate()).isEqualByComparingTo("30000");
        assertThat(saved.getSalary().getPayType()).isEqualTo(PayType.MONTHLY);
        assertThat(saved.getGovernmentId().getSssNumber()).isEqualTo("SSS-1");
        assertThat(saved.getBenefits()).hasSize(3)
                .extracting(EmployeeBenefit::getBenefit)
                .extracting(Benefit::getCode)
                .containsExactly("MEAL", "PHONE", "CLOTHING");
    }

    @Test
    void importRecord_positionNotFoundSetsPositionAndDepartmentNull() {
        EmployeeImportRecord record = EmployeeImportRecord.builder()
                .lastName("Doe")
                .firstName("Jane")
                .position("UnknownRole")
                .rate("30000")
                .payType("MONTHLY")
                .payrollFrequency("SEMI_MONTHLY")
                .status("REGULAR")
                .employmentType("FULL_TIME")
                .build();

        service.importRecord(record);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getPosition()).isNull();
        assertThat(captor.getValue().getDepartment()).isNull();
    }

    @Test
    void reasonFor_duplicateSssNumber() {
        EmployeeImportRecord record = EmployeeImportRecord.builder()
                .firstName("John")
                .lastName("Doe")
                .sssNumber("SSS-999")
                .build();

        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("could not execute statement ... Detail: Key (sss_no)=(SSS-999) already exists.");

        assertThat(service.reasonFor(ex, record)).isEqualTo("Duplicate SSS Number: SSS-999");
    }

    @Test
    void reasonFor_nonIntegrityErrorUsesMessage() {
        EmployeeImportRecord record = EmployeeImportRecord.builder().build();
        RuntimeException ex = new IllegalArgumentException("For input string: \"abc\"");

        assertThat(service.reasonFor(ex, record)).isEqualTo("For input string: \"abc\"");
    }

    @Test
    void importRecord_invalidBenefitAmountSkipsBenefitNotRow() {
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeImportRecord record = EmployeeImportRecord.builder()
                .lastName("Doe")
                .firstName("John")
                .rate("30000")
                .payType("MONTHLY")
                .payrollFrequency("SEMI_MONTHLY")
                .status("REGULAR")
                .employmentType("FULL_TIME")
                .mealAllowance("not-a-number")
                .build();

        service.importRecord(record);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getBenefits()).isEmpty();
    }
}