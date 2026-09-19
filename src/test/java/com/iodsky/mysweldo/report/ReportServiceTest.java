package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.common.StorageService;
import com.iodsky.mysweldo.payroll.run.*;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService service;

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private ReportMapper reportMapper;
    @Mock
    private StorageService storageService;
    @Mock
    private UserService userService;
    @Mock
    private PayrollReportService payrollReportService;
    @Mock
    private AttendanceReportService attendanceReportService;
    @Mock
    private CsvReportWriter csvReportWriter;
    @Mock
    private XlsxReportWriter xlsxReportWriter;

    private User superUser;
    private User payrollUser;
    private User hrUser;

    @BeforeEach
    void setUp() {
        superUser = user("SUPERUSER");
        payrollUser = user("PAYROLL");
        hrUser = user("HR");
    }

    private User user(String roleName) {
        return User.builder()
                .role(new Role(roleName))
                .build();
    }

    private PayrollRun approvedRun() {
        return PayrollRun.builder()
                .id(UUID.randomUUID())
                .status(PayrollRunStatus.APPROVED)
                .period(PayrollPeriod.of(
                        LocalDate.of(2025, 3, 1),
                        LocalDate.of(2025, 3, 15),
                        PayrollFrequency.SEMI_MONTHLY))
                .type(PayrollRunType.REGULAR)
                .build();
    }

    private Report report(ReportType type, ReportFormat format) {
        return Report.builder()
                .id(UUID.randomUUID())
                .type(type)
                .format(format)
                .fileName("x." + format.extension())
                .storageKey("reports/x." + format.extension())
                .build();
    }

    @Test
    void shouldGenerateAndPersistPayrollBankFile() {
        UUID runId = UUID.randomUUID();
        PayrollRun run = approvedRun();
        PayrollReportService.PayrollReport report = new PayrollReportService.PayrollReport(
                run, List.of("Employee ID"), List.of(List.of("10001")));

        when(payrollReportService.build(runId)).thenReturn(report);
        when(csvReportWriter.write(anyList(), anyList())).thenReturn(new byte[]{1, 2, 3});
        when(storageService.store(anyString(), anyString(), any())).thenReturn("reports/payroll.csv");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportMapper.toDto(any(Report.class))).thenReturn(ReportDto.builder().id(UUID.randomUUID()).build());

        ReportDto dto = service.createPayrollBankFile(runId, ReportFormat.CSV);

        assertThat(dto).isNotNull();
        verify(storageService).store(eq("payroll_2025-03-01_2025-03-15.csv"), eq("text/csv"), any());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(ReportType.PAYROLL_BANK_FILE);
        assertThat(saved.getFormat()).isEqualTo(ReportFormat.CSV);
        assertThat(saved.getStorageKey()).isEqualTo("reports/payroll.csv");
        assertThat(saved.getPayrollRun()).isEqualTo(run);
        assertThat(saved.getPeriodStart()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(saved.getPeriodEnd()).isEqualTo(LocalDate.of(2025, 3, 15));
    }

    @Test
    void shouldGenerateAndPersistAttendanceTimesheet() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 2);
        ReportData data = new ReportData(List.of("Date"), List.of(List.of("2026-06-01")));

        when(attendanceReportService.build(start, end)).thenReturn(data);
        when(xlsxReportWriter.write(anyList(), anyList())).thenReturn(new byte[]{4, 5, 6});
        when(storageService.store(anyString(), anyString(), any())).thenReturn("reports/att.xlsx");
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(reportMapper.toDto(any(Report.class))).thenReturn(ReportDto.builder().id(UUID.randomUUID()).build());

        ReportDto dto = service.createAttendanceTimesheet(start, end, ReportFormat.XLSX);

        assertThat(dto).isNotNull();
        verify(storageService).store(eq("attendance_2026-06-01_2026-06-02.xlsx"), eq("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"), any());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(captor.capture());
        Report saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(ReportType.ATTENDANCE_TIMESHEET);
        assertThat(saved.getFormat()).isEqualTo(ReportFormat.XLSX);
        assertThat(saved.getPayrollRun()).isNull();
        assertThat(saved.getPeriodStart()).isEqualTo(start);
        assertThat(saved.getPeriodEnd()).isEqualTo(end);
    }

    @Test
    void shouldListAllReportsForSuperuser() {
        when(userService.getAuthenticatedUser()).thenReturn(superUser);
        when(reportRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        service.getAllReports(null, 0, 10);

        verify(reportRepository).findAll(any(Pageable.class));
        verify(reportRepository, never()).findAllByType(any(), any(Pageable.class));
    }

    @Test
    void shouldListFilteredReportsForSuperuser() {
        when(userService.getAuthenticatedUser()).thenReturn(superUser);
        when(reportRepository.findAllByType(eq(ReportType.ATTENDANCE_TIMESHEET), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllReports(ReportType.ATTENDANCE_TIMESHEET, 0, 10);

        verify(reportRepository).findAllByType(eq(ReportType.ATTENDANCE_TIMESHEET), any(Pageable.class));
    }

    @Test
    void shouldScopePayrollUserToBankFiles() {
        when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
        when(reportRepository.findAllByType(eq(ReportType.PAYROLL_BANK_FILE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllReports(null, 0, 10);

        verify(reportRepository).findAllByType(eq(ReportType.PAYROLL_BANK_FILE), any(Pageable.class));
        verify(reportRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    void shouldScopeHrUserToTimesheets() {
        when(userService.getAuthenticatedUser()).thenReturn(hrUser);
        when(reportRepository.findAllByType(eq(ReportType.ATTENDANCE_TIMESHEET), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllReports(null, 0, 10);

        verify(reportRepository).findAllByType(eq(ReportType.ATTENDANCE_TIMESHEET), any(Pageable.class));
    }

    @Test
    void shouldDownloadReportBytes() {
        UUID id = UUID.randomUUID();
        Report report = report(ReportType.PAYROLL_BANK_FILE, ReportFormat.CSV);

        when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
        when(reportRepository.findById(id)).thenReturn(Optional.of(report));
        when(storageService.get("reports/x.csv")).thenReturn(new ByteArrayInputStream(new byte[]{9, 9, 9}));

        ReportService.DownloadResult result = service.download(id);

        assertThat(result.bytes()).containsExactly(9, 9, 9);
        assertThat(result.fileName()).isEqualTo("x.csv");
        assertThat(result.contentType()).isEqualTo("text/csv");
    }

    @Test
    void shouldDeleteStoredFileAndSoftDeleteRow() {
        UUID id = UUID.randomUUID();
        Report report = report(ReportType.ATTENDANCE_TIMESHEET, ReportFormat.XLSX);

        when(userService.getAuthenticatedUser()).thenReturn(hrUser);
        when(reportRepository.findById(id)).thenReturn(Optional.of(report));

        service.delete(id);

        verify(storageService).delete("reports/x.xlsx");
        assertThat(report.getDeletedAt()).isNotNull();
        verify(reportRepository).save(report);
    }

    @Test
    void shouldThrow404WhenPayrollUserAccessesAttendanceReport() {
        UUID id = UUID.randomUUID();
        Report report = report(ReportType.ATTENDANCE_TIMESHEET, ReportFormat.CSV);

        when(userService.getAuthenticatedUser()).thenReturn(payrollUser);
        when(reportRepository.findById(id)).thenReturn(Optional.of(report));

        assertThatThrownBy(() -> service.download(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldThrow404WhenReportNotFound() {
        UUID id = UUID.randomUUID();

        when(reportRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.download(id))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

}