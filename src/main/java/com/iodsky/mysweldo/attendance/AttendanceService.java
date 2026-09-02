package com.iodsky.mysweldo.attendance;

import com.iodsky.mysweldo.common.DateRange;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository repository;
    private final EmployeeService employeeService;
    private final UserService userService;
    private final AttendanceMapper attendanceMapper;

    @Transactional
    public AttendanceDto createAttendance(AttendanceRequest request) {
        Employee employee = employeeService.getEmployeeById(request.getEmployeeId());

        validateTimes(request.getTimeIn(), request.getTimeOut());

        Attendance existing = getEmployeeAttendanceByDate(employee.getId(), request.getTimeIn().toLocalDate());
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance record already exists");
        }

        BigDecimal totalHours = calculateHours(request.getTimeIn(), request.getTimeOut());

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .timeIn(request.getTimeIn())
                .timeOut(request.getTimeOut())
                .totalHours(totalHours)
                .build();

        Attendance saved = repository.save(attendance);
        return attendanceMapper.toDto(saved);
    }

    @Transactional
    public AttendanceDto clockIn() {
        Employee employee = userService.getAuthenticatedUser().getEmployee();

        boolean hasOpenAttendance = repository.existsByEmployee_IdAndTimeOutIsNull(employee.getId());
        if (hasOpenAttendance) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already clocked in for today");
        }

        Attendance attendance = repository.save(Attendance.builder()
                .employee(employee)
                .timeIn(LocalDateTime.now())
                .build());

        return attendanceMapper.toDto(attendance);
    }

    public Attendance getEmployeeAttendanceByDate(Long employeeId, LocalDate date) {
        return repository.findFirstByEmployee_IdAndTimeInBetween(
                employeeId, date.atStartOfDay(), date.plusDays(1).atStartOfDay()
        ).orElse(null);
    }

    @Transactional
    public AttendanceDto updateAttendance(UUID id, AttendanceRequest request) {
        Attendance attendance = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found with id: " + id));

        validateTimes(request.getTimeIn(), request.getTimeOut());

        BigDecimal totalHours = calculateHours(request.getTimeIn(), request.getTimeOut());

        attendance.setTimeIn(request.getTimeIn());
        attendance.setTimeOut(request.getTimeOut());
        attendance.setTotalHours(totalHours);

        Attendance updated = repository.save(attendance);
        return attendanceMapper.toDto(updated);
    }

    @Transactional
    public AttendanceDto clockOut() {
        Employee employee = userService.getAuthenticatedUser().getEmployee();
        Attendance attendance = repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByTimeInDesc(employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No open attendance record found"));
        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(attendance.getTimeIn(), now);
        if (duration.toMinutes() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot clock out within 5 minutes of clocking in. Please contact HR if this was a mistake."
            );
        }

        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        attendance.setTimeOut(now);
        attendance.setTotalHours(totalHours);

        Attendance updated = repository.save(attendance);
        return attendanceMapper.toDto(updated);
    }

    public Page<AttendanceDto> getAllAttendances(int page, int limit, LocalDate startDate, LocalDate endDate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, limit, sort);

        if (startDate == null && endDate == null) {
            Page<AttendanceView> attendances = repository.findAllBy(pageable);
            return attendances.map(attendanceMapper::toDto);
        }

        DateRange dateRange = new DateRange(startDate, endDate);
        Page<AttendanceView> attendances = repository.findAllByTimeInBetween(
                dateRange.startDate().atStartOfDay(),
                dateRange.endDate().plusDays(1).atStartOfDay(),
                pageable
        );
        return attendances.map(attendanceMapper::toDto);
    }

    public Page<AttendanceDto> getEmployeeAttendances(int page, int limit, Long employeeId, LocalDate startDate, LocalDate endDate) {
        User user = userService.getAuthenticatedUser();

        String role = user.getRole().getName();
        boolean isAdmin = role.equalsIgnoreCase("HR") || role.equalsIgnoreCase("PAYROLL");
        Long currentEmployeeId = user.getEmployee().getId();

        if (employeeId == null) {
            employeeId = currentEmployeeId;
        }

        if (!isAdmin && !employeeId.equals(currentEmployeeId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this resource");
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "timeIn");
        Pageable pageable = PageRequest.of(page, limit, sort);

        // Case 1: No date filters - return all attendances
        if (startDate == null && endDate == null) {
            Page<AttendanceView> attendances = repository.findAllByEmployee_Id(employeeId, pageable);
            return attendances.map(attendanceMapper::toDto);
        }

        // Case 2: Date filter provided - use DateRange to handle defaults
        DateRange dateRange = new DateRange(startDate, endDate);

        Page<AttendanceView> attendances = repository.findByEmployee_IdAndTimeInBetween(
                employeeId,
                dateRange.startDate().atStartOfDay(),
                dateRange.endDate().plusDays(1).atStartOfDay(),
                pageable
        );
        return attendances.map(attendanceMapper::toDto);
    }

    public List<Attendance> getEmployeeAttendances(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.findByEmployee_IdAndTimeInBetween(
                employeeId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()
        );
    }

    public boolean hasAttendance(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return !repository.findByEmployee_IdAndTimeInBetween(
                employeeId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()
        ).isEmpty();
    }

    public BigDecimal calculateTotalHoursByEmployeeId(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.sumTotalHoursByEmployee_IdAndTimeInBetween(
                employeeId, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()
        );
    }

    public AttendancePayrollSummary getAttendanceSummary(Long employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = getEmployeeAttendances(employeeId, startDate, endDate);

        long expectedWorkDays = countWorkDays(startDate, endDate);
        int daysWorked = attendances.size();
        long absenceDays = Math.max(expectedWorkDays - daysWorked, 0);

        int totalTardinessMinutes = 0;
        int totalUndertimeMinutes = 0;
        for (Attendance a: attendances) {
            Employee employee = a.getEmployee();
            if (employee == null) continue;

            LocalTime shiftStart = employee.getStartShift();
            LocalTime shiftEnd = employee.getEndShift();
            LocalTime timeIn = a.getTimeIn() == null ? null : a.getTimeIn().toLocalTime();
            LocalTime timeOut = a.getTimeOut() == null ? null : a.getTimeOut().toLocalTime();

            if (shiftStart != null && timeIn != null) {
                LocalTime tardyThreshold = shiftStart.plusMinutes(15);

                if (timeIn.isAfter(tardyThreshold)) {
                    totalTardinessMinutes += (int) Duration.between(shiftStart, timeIn).toMinutes();
                }
            }

            if (shiftEnd != null && timeOut != null) {
                LocalTime undertimeThreshold = shiftEnd.minusMinutes(15);

                if (timeOut.isBefore(undertimeThreshold)) {
                    totalUndertimeMinutes += (int) Duration.between(timeOut, shiftEnd).toMinutes();
                }
            }
        }

        return AttendancePayrollSummary.builder()
                .daysWorked(BigDecimal.valueOf(daysWorked))
                .absenceDays(BigDecimal.valueOf(absenceDays))
                .tardinessMinutes(totalTardinessMinutes)
                .undertimeMinutes(totalUndertimeMinutes)
                .build();
    }

    private long countWorkDays(LocalDate startDate, LocalDate endDate) {
        return startDate.datesUntil(endDate.plusDays(1))
                .filter(d -> !d.getDayOfWeek().equals(DayOfWeek.SATURDAY) && !d.getDayOfWeek().equals(DayOfWeek.SUNDAY))
                .count();
    }

    private void validateTimes(LocalDateTime timeIn, LocalDateTime timeOut) {
        if (timeIn == null || timeOut == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timeIn and timeOut are required");
        }

        Duration duration = Duration.between(timeIn, timeOut);
        if (duration.isNegative()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time out must be after time in");
        }

        if (duration.toMinutes() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance duration.");
        }
    }

    private BigDecimal calculateHours(LocalDateTime timeIn, LocalDateTime timeOut) {
        return BigDecimal.valueOf(Duration.between(timeIn, timeOut).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

}