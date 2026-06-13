package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.employee.Employee;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollItemServiceTest {

    @InjectMocks
    private PayrollItemService service;

    @Mock
    private PayrollItemRepository payrollRepository;

    @Mock
    private UserService userService;

    private User payrollUser;

    @BeforeEach
    void setUp() {
        Employee employee = Employee.builder().id(1L).build();

        payrollUser = User.builder()
                .id(UUID.randomUUID())
                .employee(employee)
                .role(new Role("PAYROLL"))
                .build();
    }

    @Nested
    class GetAllEmployeePayrollItemTests {

        @Test
        void shouldReturnPaginatedPayrollsForAuthenticatedEmployeeWhenNoPeriodProvided() {
            Page<PayrollItem> expectedPage = new PageImpl<>(List.of(PayrollItem.builder().build()));
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_Id(eq(1L), any(Pageable.class))).thenReturn(expectedPage);

            Page<PayrollItem> result = service.getAllEmployeePayroll(0, 10, null);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldReturnPayrollsForAuthenticatedEmployeeFilteredByPeriodWhenPeriodIsProvided() {
            YearMonth period = YearMonth.of(2025, 3);
            LocalDate expectedStart = LocalDate.of(2025, 3, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 3, 31);

            Page<PayrollItem> expectedPage = new PageImpl<>(List.of(PayrollItem.builder().build()));
            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_IdAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(expectedPage);

            Page<PayrollItem> result = service.getAllEmployeePayroll(0, 10, period);

            assertThat(result).isEqualTo(expectedPage);
        }

        @Test
        void shouldUseFirstAndLastDayOfMonthAsDateBoundsWhenFilteringEmployeePayrollByPeriod() {
            YearMonth february = YearMonth.of(2025, 2);
            LocalDate expectedStart = LocalDate.of(2025, 2, 1);
            LocalDate expectedEnd = LocalDate.of(2025, 2, 28);

            when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
            when(payrollRepository.findAllByEmployee_IdAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            service.getAllEmployeePayroll(0, 10, february);

            verify(payrollRepository).findAllByEmployee_IdAndPayrollRun_Period_StartDateLessThanEqualAndPayrollRun_Period_EndDateGreaterThanEqual(
                    eq(1L), eq(expectedEnd), eq(expectedStart), any(Pageable.class));
        }
    }
}

