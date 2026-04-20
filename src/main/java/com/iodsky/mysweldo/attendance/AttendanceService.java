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
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
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

    public AttendanceDto createAttendance(AttendanceRequest request) {
        Employee employee = employeeService.getEmployeeById(request.getEmployeeId());

        Attendance existing = getEmployeeAttendanceByDate(employee.getId(), request.getDate());
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance record already exists");
        }

        Duration duration = Duration.between(request.getTimeIn(), request.getTimeOut());
        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        if (duration.toMinutes() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance duration.");
        }

        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal regularHours = BigDecimal
                .valueOf(Duration.between(employee.getStartShift(), employee.getEndShift()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal overtime = totalHours.subtract(regularHours);
        if (overtime.compareTo(BigDecimal.ZERO) < 0) {
            overtime = BigDecimal.ZERO;
        }

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(request.getDate())
                .timeIn(request.getTimeIn())
                .timeOut(request.getTimeOut())
                .totalHours(totalHours)
                .overtime(overtime)
                .build();

        Attendance saved = repository.save(attendance);
        return attendanceMapper.toDto(saved);
    }

    public AttendanceDto clockIn() {
        Employee employee = userService.getAuthenticatedUser().getEmployee();
        LocalDate today = LocalDate.now();

        boolean hasOpenAttendance = repository.existsByEmployee_IdAndTimeOutIsNull(employee.getId());
        if (hasOpenAttendance) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already clocked in for today");
        }

        Attendance attendance = repository.save(Attendance.builder()
                .employee(employee)
                .date(today)
                .timeIn(LocalTime.now())
                .build());

        return attendanceMapper.toDto(attendance);
    }

    public Attendance getEmployeeAttendanceByDate(Long employeeId, LocalDate date) {
        return repository.findByEmployee_IdAndDate(employeeId, date).orElse(null);
    }

    public AttendanceDto updateAttendance(UUID id, AttendanceRequest request) {
        Attendance attendance = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found with id: " + id));
        Employee employee = attendance.getEmployee();

        Duration duration = Duration.between(request.getTimeIn(), request.getTimeOut());
        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        if (duration.toMinutes() < 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid attendance duration.");
        }

        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal regularHours = BigDecimal
                .valueOf(Duration.between(employee.getStartShift(), employee.getEndShift()).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal overtime = totalHours.subtract(regularHours);
        if (overtime.compareTo(BigDecimal.ZERO) < 0) {
            overtime = BigDecimal.ZERO;
        }

        attendance.setDate(request.getDate());
        attendance.setTimeIn(request.getTimeIn());
        attendance.setTimeOut(request.getTimeOut());
        attendance.setTotalHours(totalHours);
        attendance.setOvertime(overtime);

        Attendance updated = repository.save(attendance);
        return attendanceMapper.toDto(updated);
    }

    public AttendanceDto clockOut() {
        Employee employee = userService.getAuthenticatedUser().getEmployee();
        Attendance attendance = repository.findFirstByEmployee_IdAndTimeOutIsNullOrderByDateDescTimeInDesc(employee.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No open attendance record found"));
        LocalTime now = LocalTime.now();

        Duration duration = Duration.between(attendance.getTimeIn(), now);
        if (duration.isNegative()) {
            duration = duration.plusHours(24);
        }

        if (duration.toMinutes() < 5) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot clock out within 5 minutes of clocking in. Please contact HR if this was a mistake."
            );
        }

        BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        LocalTime startShift = employee.getStartShift();
        LocalTime endShift = employee.getEndShift();
        if (startShift == null || endShift == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee shift times are not configured. Cannot calculate hours.");
        }

        BigDecimal regularHours = BigDecimal
                .valueOf(Duration.between(startShift, endShift).toMinutes())
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

        BigDecimal overtime = totalHours.subtract(regularHours);
        if (overtime.compareTo(BigDecimal.ZERO) < 0) {
            overtime = BigDecimal.ZERO;
        }

        attendance.setTimeOut(now);
        attendance.setTotalHours(totalHours);
        attendance.setOvertime(overtime);

        Attendance updated = repository.save(attendance);
        return attendanceMapper.toDto(updated);
    }

    public Page<AttendanceDto> getAllAttendances(int page, int limit, LocalDate startDate, LocalDate endDate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, limit, sort);

        if (startDate == null && endDate == null) {
            Page<Attendance> attendances = repository.findAll(pageable);
            return attendances.map(attendanceMapper::toDto);
        }

        DateRange dateRange = new DateRange(startDate, endDate);
        Page<Attendance> attendances = repository.findAllByDateBetween(dateRange.startDate(), dateRange.endDate(), pageable);
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

        Sort sort = Sort.by(Sort.Direction.DESC, "date");
        Pageable pageable = PageRequest.of(page, limit, sort);

        // Case 1: No date filters - return all attendances
        if (startDate == null && endDate == null) {
            Page<Attendance> attendances = repository.findAllByEmployee_Id(employeeId, pageable);
            return attendances.map(attendanceMapper::toDto);
        }

        // Case 2: Date filter provided - use DateRange to handle defaults
        // - If only startDate: return attendances from startDate onwards
        // - If only endDate: return attendances up to endDate
        // - If both: return attendances within the range
        DateRange dateRange = new DateRange(startDate, endDate);

        Page<Attendance> attendances = repository.findByEmployee_IdAndDateBetween(employeeId, dateRange.startDate(), dateRange.endDate(), pageable);
        return attendances.map(attendanceMapper::toDto);
    }

    public List<Attendance> getEmployeeAttendances(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.findByEmployee_IdAndDateBetween(employeeId, startDate, endDate);
    }

    public boolean hasAttendance(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return !repository.findByEmployee_IdAndDateBetween(employeeId, startDate, endDate).isEmpty();
    }

    public BigDecimal calculateTotalHoursByEmployeeId(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.sumTotalHoursByEmployee_IdAndDateBetween(employeeId, startDate, endDate);
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
            LocalTime timeIn = a.getTimeIn();
            LocalTime timeOut = a.getTimeOut();

            if (shiftStart != null && timeIn !=null) {
                LocalTime tardyThreshold = shiftStart.plusMinutes(15);

                if (timeIn.isAfter(tardyThreshold)) {
                    totalTardinessMinutes += (int) Duration.between(shiftStart, timeIn).toMinutes();
                }
            }

            if (shiftEnd != null && timeOut != null && !LocalTime.MIN.equals(timeOut)) {
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

}
