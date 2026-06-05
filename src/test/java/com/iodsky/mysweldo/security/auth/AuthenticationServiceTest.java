package com.iodsky.mysweldo.security.auth;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.jwt.JwtService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserDto;
import com.iodsky.mysweldo.security.user.UserMapper;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private JwtService jwtService;

    @Mock
    private UserMapper userMapper;

    private AuthRequest validLoginRequest;
    private User validUser;
    private UserDto validUserDto;

    @BeforeEach
    void setUp() {
        Employee employee = Employee.builder().id(10000L).build();
        Role employeeRole = Role.builder().name("EMPLOYEE").build();

        validLoginRequest = AuthRequest.builder()
                .email("john@example.com")
                .password("secret123")
                .accessType(AccessType.EMPLOYEE)
                .build();

        validUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .email("john@example.com")
                .role(employeeRole)
                .build();

        validUserDto = UserDto.builder()
                .id(validUser.getId())
                .email("john@example.com")
                .employeeId(10000L)
                .role("EMPLOYEE")
                .build();
    }

    @Nested
    class AuthenticateTests {

        @Test
        void shouldReturnLoginResponseWhenCredentialsAreValid() {
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);
            when(jwtService.generateAccessToken(any(), any()))
                    .thenReturn("mocked.access.token");
            when(userMapper.toDto(validUser)).thenReturn(validUserDto);

            AuthSession response = authenticationService.authenticate(validLoginRequest);

            assertThat(response).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");
            assertThat(response.getUser().getRole()).isEqualTo("EMPLOYEE");
            assertThat(response.getToken()).isEqualTo("mocked.access.token");
        }

        @Test
        void shouldPropagateBadCredentialsException() {
            AuthRequest invalidLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("wrongpassword")
                    .accessType(AccessType.EMPLOYEE)
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authenticationService.authenticate(invalidLoginRequest))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        void shouldThrowForbiddenWhenAdminAccessDeniedForEmployee() {
            AuthRequest adminLoginRequest = AuthRequest.builder()
                    .email("john@example.com")
                    .password("secret123")
                    .accessType(AccessType.ADMIN)
                    .build();

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userService.getUserByEmail("john@example.com")).thenReturn(validUser);

            assertThatThrownBy(() -> authenticationService.authenticate(adminLoginRequest))
                    .isInstanceOf(ResponseStatusException.class)
                    .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                    .isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
