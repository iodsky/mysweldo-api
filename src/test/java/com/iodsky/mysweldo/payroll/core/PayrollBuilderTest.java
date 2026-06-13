package com.iodsky.mysweldo.payroll.core;

import com.iodsky.mysweldo.contribution.ContributionService;
import com.iodsky.mysweldo.deduction.DeductionService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Salary;
import com.iodsky.mysweldo.payroll.run.PayrollFrequency;
import com.iodsky.mysweldo.payroll.run.PayrollPeriod;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import com.iodsky.mysweldo.payroll.run.PayrollRunException;
import com.iodsky.mysweldo.payroll.strategy.PayrollComputationStrategy;
import com.iodsky.mysweldo.payroll.strategy.PayrollStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollBuilderTest {

    @InjectMocks
    private PayrollBuilder builder;

    @Mock private EmployeeService employeeService;
    @Mock private DeductionService deductionService;
    @Mock private ContributionService contributionService;
    @Mock private PayrollStrategyFactory strategyFactory;
    @Mock private PayrollComputationStrategy strategy;
    @Mock private PayrollConfiguration config;

    private PayrollRun semiMonthlyRun;

    @BeforeEach
    void setUp() {
        semiMonthlyRun = PayrollRun.builder()
                .period(PayrollPeriod.of(
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 3, 15),
                        PayrollFrequency.SEMI_MONTHLY))
                .build();
    }

    private Employee employeeWithFrequency(PayrollFrequency frequency) {
        return Employee.builder()
                .id(1L)
                .salary(Salary.builder()
                        .rate(BigDecimal.valueOf(20000))
                        .payrollFrequency(frequency)
                        .build())
                .benefits(List.of())
                .build();
    }

    @Nested
    class FrequencyReconciliation {

        @Test
        void shouldProceedWhenEmployeeFrequencyMatchesRun() {
            Employee employee = employeeWithFrequency(PayrollFrequency.SEMI_MONTHLY);
            PayrollContext context = mock(PayrollContext.class);

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(strategyFactory.getStrategy(PayrollFrequency.SEMI_MONTHLY)).thenReturn(strategy);
            when(strategy.compute(any(), any(), any())).thenReturn(context);
            when(context.getEmployee()).thenReturn(employee);
            when(context.getEmployeeBenefits()).thenReturn(List.of());
            when(context.getOvertimeHours()).thenReturn(BigDecimal.ZERO);
            when(context.getGrossPay()).thenReturn(BigDecimal.ZERO);
            when(context.getTotalBenefits()).thenReturn(BigDecimal.ZERO);
            when(context.getTotalDeductions()).thenReturn(BigDecimal.ZERO);
            when(context.getNetPay()).thenReturn(BigDecimal.ZERO);
            when(context.getSss()).thenReturn(BigDecimal.ZERO);
            when(context.getPhilhealth()).thenReturn(BigDecimal.ZERO);
            when(context.getPagibig()).thenReturn(BigDecimal.ZERO);
            when(context.getWithholdingTax()).thenReturn(BigDecimal.ZERO);
            when(context.getSssEr()).thenReturn(BigDecimal.ZERO);
            when(context.getPhilhealthEr()).thenReturn(BigDecimal.ZERO);
            when(context.getPagibigEr()).thenReturn(BigDecimal.ZERO);
            when(deductionService.getDeductionByCode(any())).thenReturn(null);
            when(contributionService.getContributionByCode(any())).thenReturn(null);

            assertThatNoException().isThrownBy(() -> builder.buildPayroll(1L, semiMonthlyRun, config));
            verify(strategy).compute(employee, semiMonthlyRun, config);
        }

        @Test
        void shouldThrowWhenEmployeeFrequencyDoesNotMatchRun() {
            Employee employee = employeeWithFrequency(PayrollFrequency.WEEKLY);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);

            assertThatThrownBy(() -> builder.buildPayroll(1L, semiMonthlyRun, config))
                    .isInstanceOf(PayrollRunException.class)
                    .hasMessageContaining("WEEKLY")
                    .hasMessageContaining("SEMI_MONTHLY");

            verify(strategy, never()).compute(any(), any(), any());
        }

        @Test
        void shouldProceedWithWarnWhenEmployeeFrequencyIsNull() {
            Employee employee = Employee.builder()
                    .id(1L)
                    .salary(Salary.builder()
                            .rate(BigDecimal.valueOf(20000))
                            .payrollFrequency(null)
                            .build())
                    .benefits(List.of())
                    .build();
            PayrollContext context = mock(PayrollContext.class);

            when(employeeService.getEmployeeById(1L)).thenReturn(employee);
            when(strategyFactory.getStrategy(PayrollFrequency.SEMI_MONTHLY)).thenReturn(strategy);
            when(strategy.compute(any(), any(), any())).thenReturn(context);
            when(context.getEmployee()).thenReturn(employee);
            when(context.getEmployeeBenefits()).thenReturn(List.of());
            when(context.getOvertimeHours()).thenReturn(BigDecimal.ZERO);
            when(context.getGrossPay()).thenReturn(BigDecimal.ZERO);
            when(context.getTotalBenefits()).thenReturn(BigDecimal.ZERO);
            when(context.getTotalDeductions()).thenReturn(BigDecimal.ZERO);
            when(context.getNetPay()).thenReturn(BigDecimal.ZERO);
            when(context.getSss()).thenReturn(BigDecimal.ZERO);
            when(context.getPhilhealth()).thenReturn(BigDecimal.ZERO);
            when(context.getPagibig()).thenReturn(BigDecimal.ZERO);
            when(context.getWithholdingTax()).thenReturn(BigDecimal.ZERO);
            when(context.getSssEr()).thenReturn(BigDecimal.ZERO);
            when(context.getPhilhealthEr()).thenReturn(BigDecimal.ZERO);
            when(context.getPagibigEr()).thenReturn(BigDecimal.ZERO);
            when(deductionService.getDeductionByCode(any())).thenReturn(null);
            when(contributionService.getContributionByCode(any())).thenReturn(null);

            assertThatNoException().isThrownBy(() -> builder.buildPayroll(1L, semiMonthlyRun, config));
            verify(strategy).compute(employee, semiMonthlyRun, config);
        }

        @Test
        void shouldThrowForEachMismatchedFrequency() {
            PayrollRun monthlyRun = PayrollRun.builder()
                    .period(PayrollPeriod.of(
                            LocalDate.of(2025, 3, 1),
                            LocalDate.of(2025, 3, 31),
                            PayrollFrequency.MONTHLY))
                    .build();

            Employee employee = employeeWithFrequency(PayrollFrequency.BI_WEEKLY);
            when(employeeService.getEmployeeById(1L)).thenReturn(employee);

            assertThatThrownBy(() -> builder.buildPayroll(1L, monthlyRun, config))
                    .isInstanceOf(PayrollRunException.class)
                    .hasMessageContaining("BI_WEEKLY")
                    .hasMessageContaining("MONTHLY");
        }
    }
}
