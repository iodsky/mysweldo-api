package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.jwt.JwtUtil;
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
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserDetailsService userDetailsService;

    private LoginRequest validLoginRequest;
    private User validUser;
    private Role employeeRole;
    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = Employee.builder().id(10000L).build();
        employeeRole = Role.builder().name("EMPLOYEE").build();

        validLoginRequest = LoginRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .role("EMPLOYEE")
                .build();

        validUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .email("john@example.com")
                .role(employeeRole)
                .build();
    }

    @Nested
    class AuthenticateTests {

        @Test
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            LoginResponse response = authenticationService.authenticate(validLoginRequest);

            assertNotNull(response);
            assertEquals("john@example.com", response.getEmail());
            assertEquals("EMPLOYEE", response.getRole());
        }

        @Test
        void shouldPropagateBadCredentialsException() {
            LoginRequest invalidLoginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .role("EMPLOYEE")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThrows(
                    BadCredentialsException.class,
                    () -> authenticationService.authenticate(invalidLoginRequest)
            );
        }

        @Test
        void shouldReturnResponseBelongingToAuthenticatedUser() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            LoginResponse response = authenticationService.authenticate(validLoginRequest);

            assertEquals("john@example.com", response.getEmail());
        }

        @Test
        void shouldThrowForbiddenWhenAdminAccessDeniedForEmployee() {
            LoginRequest adminLoginRequest = LoginRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .role("ADMIN")
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> authenticationService.authenticate(adminLoginRequest)
            );

            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        }
    }

    @Nested
    class GenerateTokenTests {

        @Test
        void shouldGenerateTokenForValidEmail() {
            String email = "john@example.com";
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("encoded")
                    .build();

            when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails)).thenReturn("mocked.jwt.token");

            String token = authenticationService.generateToken(email);

            assertNotNull(token);
            assertEquals("mocked.jwt.token", token);
        }
    }
}

