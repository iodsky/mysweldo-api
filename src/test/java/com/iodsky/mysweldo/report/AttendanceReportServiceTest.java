package com.iodsky.mysweldo.report;

import com.iodsky.mysweldo.attendance.Attendance;
import com.iodsky.mysweldo.attendance.AttendanceRepository;
import com.iodsky.mysweldo.employee.Employee;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceReportServiceTest {

    @InjectMocks
    private AttendanceReportService service;

    @Mock
    private AttendanceRepository attendanceRepository;

    private Employee employee(String firstName, String lastName) {
        return Employee.builder()
                .id(1L)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }

    private Attendance record(Employee employee, LocalDateTime timeIn, LocalDateTime timeOut) {
        return Attendance.builder()
                .employee(employee)
                .timeIn(timeIn)
                .timeOut(timeOut)
                .totalHours(new BigDecimal("9.00"))
                .build();
    }

    @Test
    void shouldBuildTimesheetRowsForDateRange() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 2);
        Employee employee = employee("John", "Doe");
        Attendance first = record(employee, LocalDateTime.of(2026, 6, 1, 9, 0), LocalDateTime.of(2026, 6, 1, 18, 0));
        Attendance second = record(employee, LocalDateTime.of(2026, 6, 2, 9, 0), LocalDateTime.of(2026, 6, 2, 18, 0));

        when(attendanceRepository.findAllByTimeInBetweenOrderByTimeInAsc(start.atStartOfDay(), end.plusDays(1).atStartOfDay()))
                .thenReturn(List.of(first, second));

        ReportData data = service.build(start, end);

        assertThat(data.headers()).containsExactly("Date", "Employee Name", "Time In", "Time Out", "Total Hours");
        assertThat(data.rows()).hasSize(2);
        assertThat(data.rows().getFirst())
                .containsExactly("2026-06-01", "John Doe", "09:00", "18:00", "9.00");
        assertThat(data.rows().get(1))
                .containsExactly("2026-06-02", "John Doe", "09:00", "18:00", "9.00");
    }

    @Test
    void shouldReturnHeadersOnlyWhenNoRecords() {
        LocalDate start = LocalDate.of(2026, 6, 1);
        LocalDate end = LocalDate.of(2026, 6, 2);

        when(attendanceRepository.findAllByTimeInBetweenOrderByTimeInAsc(any(), any())).thenReturn(List.of());

        ReportData data = service.build(start, end);

        assertThat(data.headers()).containsExactly("Date", "Employee Name", "Time In", "Time Out", "Total Hours");
        assertThat(data.rows()).isEmpty();
    }

    @Test
    void shouldThrow400WhenDatesAreNull() {
        assertThatThrownBy(() -> service.build(null, null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrow400WhenStartDateIsAfterEndDate() {
        assertThatThrownBy(() -> service.build(LocalDate.of(2026, 6, 2), LocalDate.of(2026, 6, 1)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

}