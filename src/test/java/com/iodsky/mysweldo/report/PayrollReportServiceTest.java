package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.item.PayrollItem;
import com.iodsky.mysweldo.payroll.item.PayrollItemRepository;
import com.iodsky.mysweldo.payroll.run.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollReportServiceTest {

    @InjectMocks
    private PayrollReportService service;

    @Mock
    private PayrollRunRepository payrollRunRepository;

    @Mock
    private PayrollItemRepository payrollItemRepository;

    private PayrollRun run(PayrollRunStatus status) {
        return PayrollRun.builder()
                .id(UUID.randomUUID())
                .status(status)
                .period(PayrollPeriod.of(
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 3, 15),
                        PayrollFrequency.SEMI_MONTHLY))
                .type(PayrollRunType.REGULAR)
                .build();
    }

    private PayrollItem item(Long employeeId, String firstName, String lastName, String bank, String account, BigDecimal netPay) {
        Employee employee = Employee.builder()
                .id(employeeId)
                .firstName(firstName)
                .lastName(lastName)
                .bankName(bank)
                .accountNumber(account)
                .build();
        return PayrollItem.builder()
                .employee(employee)
                .netPay(netPay)
                .build();
    }

    @Test
    void shouldBuildBankFileRowsForApprovedRun() {
        UUID runId = UUID.randomUUID();
        PayrollRun run = run(PayrollRunStatus.APPROVED);
        PayrollItem item = item(10001L, "John", "Doe", "BDO", "1234567890", new BigDecimal("25000.00"));

        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));

        PayrollReportService.PayrollReport report = service.build(runId);

        assertThat(report.run()).isEqualTo(run);
        assertThat(report.headers()).containsExactly("Employee ID", "Employee Name", "Bank Name", "Account Number", "Net Pay");
        assertThat(report.rows()).hasSize(1);
        assertThat(report.rows().getFirst())
                .containsExactly("10001", "John Doe", "BDO", "1234567890", "25000.00");
    }

    @Test
    void shouldBuildBankFileRowsForProcessedRun() {
        UUID runId = UUID.randomUUID();
        PayrollRun run = run(PayrollRunStatus.PROCESSED);
        PayrollItem item = item(10002L, "Jane", "Smith", null, null, new BigDecimal("18000.50"));

        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of(item));

        PayrollReportService.PayrollReport report = service.build(runId);

        assertThat(report.rows().getFirst())
                .containsExactly("10002", "Jane Smith", "", "", "18000.50");
    }

    @Test
    void shouldThrow400WhenRunIsDraft() {
        UUID runId = UUID.randomUUID();
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run(PayrollRunStatus.DRAFT)));

        assertThatThrownBy(() -> service.build(runId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrow400WhenRunHasNoItems() {
        UUID runId = UUID.randomUUID();
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.of(run(PayrollRunStatus.APPROVED)));
        when(payrollItemRepository.findAllByPayrollRun_Id(runId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.build(runId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrow404WhenRunNotFound() {
        UUID runId = UUID.randomUUID();
        when(payrollRunRepository.findById(runId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.build(runId))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

}