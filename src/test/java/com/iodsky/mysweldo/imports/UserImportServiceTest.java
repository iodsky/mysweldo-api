package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.role.RoleRepository;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTest {

    @InjectMocks
    private UserImportService service;

    @Mock
    private ImportJobRepository importJobRepository;

    @Mock
    private ImportJobErrorRepository importJobErrorRepository;

    @Mock
    private EmployeeService employeeService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private Role hrRole;

    @BeforeEach
    void setUp() {
        hrRole = Role.builder().id(1L).name("HR").build();
        lenient().when(roleRepository.findAll()).thenReturn(List.of(hrRole));
        lenient().when(passwordEncoder.encode("secret")).thenReturn("encoded-secret");
    }

    @Test
    void importRecord_mapsCsvRowToUser() {
        Employee employee = Employee.builder().id(1L).build();
        when(employeeService.getEmployeeById(1L)).thenReturn(employee);

        UserImportRecord record = UserImportRecord.builder()
                .employeeId("1")
                .role("HR")
                .email("john@example.com")
                .password("secret")
                .build();

        service.importRecord(record);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getEmployee()).isSameAs(employee);
        assertThat(saved.getRole()).isSameAs(hrRole);
        assertThat(saved.getPassword()).isEqualTo("encoded-secret");
    }

    @Test
    void importRecord_unknownRoleThrows() {
        when(employeeService.getEmployeeById(1L)).thenReturn(Employee.builder().id(1L).build());

        UserImportRecord record = UserImportRecord.builder()
                .employeeId("1")
                .role("UNKNOWN")
                .email("jane@example.com")
                .password("secret")
                .build();

        assertThatThrownBy(() -> service.importRecord(record))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid user role: UNKNOWN");
    }

    @Test
    void importRecord_missingEmployeeThrows() {
        when(employeeService.getEmployeeById(99L))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Employee 99 not found"));

        UserImportRecord record = UserImportRecord.builder()
                .employeeId("99")
                .role("HR")
                .email("ghost@example.com")
                .password("secret")
                .build();

        assertThatThrownBy(() -> service.importRecord(record))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Employee 99 not found");
    }

    @Test
    void reasonFor_duplicateEmail() {
        UserImportRecord record = UserImportRecord.builder().email("dup@example.com").build();

        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("could not execute statement ... Key (email)=(dup@example.com) already exists.");

        assertThat(service.reasonFor(ex, record)).isEqualTo("Duplicate email: dup@example.com");
    }
}