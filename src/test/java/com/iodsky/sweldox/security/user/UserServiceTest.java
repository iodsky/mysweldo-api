package com.iodsky.sweldox.security.user;

import com.iodsky.sweldox.employee.EmployeeService;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.security.role.Role;
import com.iodsky.sweldox.security.role.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleService roleService;
    @Mock private UserMapper userMapper;
    @Mock private EmployeeService employeeService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;

    private User user;
    private UserRequest userRequest;
    private Employee employee;
    private Role role;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);

        role = Role.builder()
                .id(1L)
                .name("HR")
                .build();

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("john.doe@example.com");
        user.setPassword("encoded-pass");
        user.setEmployee(employee);
        user.setRole(role);

        userRequest = new UserRequest();
        userRequest.setEmployeeId(1L);
        userRequest.setPassword("password123");
        userRequest.setRole("HR");
    }

    @Nested
    class LoadUserByUsernameTests {
        @Test
        void shouldReturnUserDetailsWhenUserExists() {
            when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(user));

            UserDetails result = userService.loadUserByUsername("john.doe@example.com");

            assertNotNull(result);
            assertEquals("john.doe@example.com", result.getUsername());
            verify(userRepository).findByEmail("john.doe@example.com");
        }

        @Test
        void shouldThrowNotFoundExceptionWhenUserDoesNotExist() {
            when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.loadUserByUsername("missing@example.com"));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("User missing@example.com not found", ex.getReason());
        }

        @Test
        void shouldHandleNullUsernameGracefully() {
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.loadUserByUsername(null));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    @Nested
    class GetAllUsersTests {

        @Test
        void shouldReturnAllUsersWhenRoleIsNull() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

            Page<User> result = userService.getAllUsers(0, 10, null);

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            verify(userRepository).findAll(any(Pageable.class));
            verifyNoInteractions(roleService);
        }

        @Test
        void shouldReturnUsersByRoleWhenValidRoleExists() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            when(userRepository.findAllByRole_Name(eq("HR"), any(Pageable.class))).thenReturn(userPage);

            Page<User> result = userService.getAllUsers(0, 10, "HR");

            assertEquals(1, result.getTotalElements());
            assertEquals(1, result.getContent().size());
            verify(userRepository).findAllByRole_Name(eq("HR"), any(Pageable.class));
        }

    }

    @Nested
    class CreateUserTests {

        @Test
        void shouldCreateUserSuccessfully() {
            when(userMapper.toEntity(any(UserRequest.class))).thenReturn(user);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(roleService.getRoleByName("HR")).thenReturn(role);
            when(passwordEncoder.encode("password123")).thenReturn("encoded-pass");
            when(userRepository.save(any(User.class))).thenReturn(user);

            User result = userService.createUser(userRequest);

            assertNotNull(result);
            assertEquals("encoded-pass", result.getPassword());
            assertEquals(employee, result.getEmployee());
            assertEquals(role, result.getRole());
            verify(userRepository).save(any(User.class));
        }

        @Test
        void shouldThrowNotFoundWhenEmployeeDoesNotExist() {
            when(userMapper.toEntity(any(UserRequest.class))).thenReturn(user);
            when(employeeService.getEmployeeById(1L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(userRequest));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldThrowNotFoundWhenRoleNotFound() {
            when(userMapper.toEntity(any(UserRequest.class))).thenReturn(user);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(roleService.getRoleByName("HR")).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Role HR not found"));

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> userService.createUser(userRequest));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        void shouldEncodePasswordBeforeSaving() {
            when(userMapper.toEntity(any(UserRequest.class))).thenReturn(user);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(roleService.getRoleByName("HR")).thenReturn(role);
            when(passwordEncoder.encode("password123")).thenReturn("ENCODED123");
            when(userRepository.save(any(User.class))).thenReturn(user);

            User result = userService.createUser(userRequest);

            verify(passwordEncoder).encode("password123");
            assertEquals("ENCODED123", result.getPassword()); // the user object was preset
        }

        @Test
        void shouldPropagateRepositoryErrors() {
            when(userMapper.toEntity(any(UserRequest.class))).thenReturn(user);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(roleService.getRoleByName("HR")).thenReturn(role);
            when(passwordEncoder.encode(anyString())).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenThrow(new RuntimeException("DB failure"));

            assertThrows(RuntimeException.class, () -> userService.createUser(userRequest));
        }
    }

}
