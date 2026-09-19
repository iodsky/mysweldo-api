package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.attendance.AttendanceRepository;
import com.iodsky.mysweldo.employee.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceReportService {

    private static final List<String> HEADERS = List.of(
            "Date", "Employee Name", "Time In", "Time Out", "Total Hours");

    private final AttendanceRepository attendanceRepository;

    public ReportData build(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate and endDate are required");
        }
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startDate must be on or before endDate");
        }

        List<Attendance> records = attendanceRepository
                .findAllByTimeInBetweenOrderByTimeInAsc(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());

        List<List<String>> rows = records.stream()
                .map(this::toRow)
                .toList();

        return new ReportData(HEADERS, rows);
    }

    private List<String> toRow(Attendance attendance) {
        Employee employee = attendance.getEmployee();
        return List.of(
                attendance.getTimeIn() == null ? "" : attendance.getTimeIn().toLocalDate().toString(),
                employee == null ? "" : employee.getFirstName() + " " + employee.getLastName(),
                attendance.getTimeIn() == null ? "" : attendance.getTimeIn().toLocalTime().toString(),
                attendance.getTimeOut() == null ? "" : attendance.getTimeOut().toLocalTime().toString(),
                attendance.getTotalHours() == null ? "" : attendance.getTotalHours().toPlainString()
        );
    }

}