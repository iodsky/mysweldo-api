package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.common.StorageService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportMapper reportMapper;
    private final StorageService storageService;
    private final UserService userService;
    private final PayrollReportService payrollReportService;
    private final AttendanceReportService attendanceReportService;
    private final CsvReportWriter csvReportWriter;
    private final XlsxReportWriter xlsxReportWriter;

    @Transactional
    public ReportDto createPayrollBankFile(UUID runId, ReportFormat format) {
        PayrollReportService.PayrollReport report = payrollReportService.build(runId);
        byte[] bytes = writer(format).write(report.headers(), report.rows());

        String fileName = "payroll_" + report.run().getPeriod().getStartDate()
                + "_" + report.run().getPeriod().getEndDate() + "." + format.extension();

        Report entity = Report.builder()
                .type(ReportType.PAYROLL_BANK_FILE)
                .format(format)
                .fileName(fileName)
                .storageKey(storageService.store(fileName, format.contentType(), bytes))
                .payrollRun(report.run())
                .periodStart(report.run().getPeriod().getStartDate())
                .periodEnd(report.run().getPeriod().getEndDate())
                .build();

        return reportMapper.toDto(reportRepository.save(entity));
    }

    @Transactional
    public ReportDto createAttendanceTimesheet(LocalDate startDate, LocalDate endDate, ReportFormat format) {
        ReportData reportData = attendanceReportService.build(startDate, endDate);
        byte[] bytes = writer(format).write(reportData.headers(), reportData.rows());

        String fileName = "attendance_" + startDate + "_" + endDate + "." + format.extension();

        Report entity = Report.builder()
                .type(ReportType.ATTENDANCE_TIMESHEET)
                .format(format)
                .fileName(fileName)
                .storageKey(storageService.store(fileName, format.contentType(), bytes))
                .periodStart(startDate)
                .periodEnd(endDate)
                .build();

        return reportMapper.toDto(reportRepository.save(entity));
    }

    public Page<ReportDto> getAllReports(ReportType type, int pageNo, int limit) {
        Pageable pageable = PageRequest.of(pageNo, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        String role = currentRole();

        Page<Report> page = switch (role) {
            case "SUPERUSER" -> type != null
                    ? reportRepository.findAllByType(type, pageable)
                    : reportRepository.findAll(pageable);
            case "PAYROLL" -> reportRepository.findAllByType(ReportType.PAYROLL_BANK_FILE, pageable);
            case "HR" -> reportRepository.findAllByType(ReportType.ATTENDANCE_TIMESHEET, pageable);
            default -> throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        };

        return page.map(reportMapper::toDto);
    }

    @Transactional(readOnly = true)
    public DownloadResult download(UUID id) {
        Report report = getAccessibleReport(id);
        try (InputStream in = storageService.get(report.getStorageKey())) {
            return new DownloadResult(in.readAllBytes(), report.getFileName(), report.getFormat().contentType());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read report file: " + id, e);
        }
    }

    @Transactional
    public void delete(UUID id) {
        Report report = getAccessibleReport(id);
        storageService.delete(report.getStorageKey());
        report.setDeletedAt(Instant.now());
        reportRepository.save(report);
    }

    private Report getAccessibleReport(UUID id) {
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report " + id + " not found"));

        String role = currentRole();
        boolean allowed = switch (role) {
            case "SUPERUSER" -> true;
            case "PAYROLL" -> report.getType() == ReportType.PAYROLL_BANK_FILE;
            case "HR" -> report.getType() == ReportType.ATTENDANCE_TIMESHEET;
            default -> false;
        };

        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report " + id + " not found");
        }
        return report;
    }

    private String currentRole() {
        User user = userService.getAuthenticatedUser();
        Role role = user.getRole();
        return role == null ? "" : role.getName();
    }

    private ReportWriter writer(ReportFormat format) {
        return switch (format) {
            case CSV -> csvReportWriter;
            case XLSX -> xlsxReportWriter;
        };
    }

    public record DownloadResult(byte[] bytes, String fileName, String contentType) {
    }

}