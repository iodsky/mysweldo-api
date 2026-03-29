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

    public Attendance createAttendance(AttendanceDto attendanceDto) {
        User user = userService.getAuthenticatedUser();

        boolean isHr = "HR".equalsIgnoreCase(user.getRole().getName());

        // All roles may clock themselves in, but only HR can add for others
        Long currentEmployeeId = user.getEmployee().getId();

        // Determine target employee
        Long employeeId;
        if (attendanceDto == null || attendanceDto.getEmployeeId() == null) {
            // Self clock-in
            employeeId = currentEmployeeId;
        } else {
            employeeId = attendanceDto.getEmployeeId();

            // Authorization rule
            if (!isHr && !employeeId.equals(currentEmployeeId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have the permissions to access this resource");
            }
        }

        // Determine attendance date and time
        LocalDate attendanceDate = (attendanceDto != null && attendanceDto.getDate() != null)
                ? attendanceDto.getDate()
                : LocalDate.now();

        LocalTime clockInTime = (attendanceDto != null && attendanceDto.getTimeIn() != null)
                ? attendanceDto.getTimeIn()
                : LocalTime.now();

        // Check for existing attendance record
        Attendance existing = getEmployeeAttendanceByDate(employeeId, attendanceDate);
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Attendance record already exists");
        }

        // Build attendance
        Employee employee = employeeService.getEmployeeById(employeeId);

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .date(attendanceDate)
                .timeIn(clockInTime)
                .timeOut(LocalTime.MIN)
                .totalHours(BigDecimal.ZERO)
                .overtime(BigDecimal.ZERO)
                .build();

        return repository.save(attendance);
    }

    public Attendance getEmployeeAttendanceByDate(Long employeeId, LocalDate date) {
        return repository.findByEmployee_IdAndDate(employeeId, date).orElse(null);
    }

    public Attendance updateAttendance(UUID id, AttendanceDto attendanceDto) {
        User user = userService.getAuthenticatedUser();

        boolean isHr = "HR".equalsIgnoreCase(user.getRole().getName());

        Attendance attendance = repository.findById(id)
                // Not yet clocked in
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attendance not found with id: " + id));

        long currentEmpId = user.getEmployee().getId();
        long employeeId = attendance.getEmployee().getId();

        if (!isHr && employeeId != currentEmpId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have the permissions to access this resource");
        }

        if (attendanceDto == null) {
            if (attendance.getTimeOut() != null && !attendance.getTimeOut().equals(LocalTime.MIN)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You have already clocked out for the day.");
            }

            attendance.setTimeOut(LocalTime.now());
        }
        else {
            if (!isHr) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have the permissions to access this resource");
            }

            if (attendanceDto.getTimeIn() != null) {
                attendance.setTimeIn(attendanceDto.getTimeIn());
            }

            if (attendanceDto.getTimeOut() != null) {
                attendance.setTimeOut(attendanceDto.getTimeOut());
            }

            if (attendanceDto.getDate() != null) {
                attendance.setDate(attendanceDto.getDate());
            }

            LocalTime effectiveTimeIn = attendance.getTimeIn();
            LocalTime effectiveTimeOut = attendance.getTimeOut();
            if (effectiveTimeIn != null && effectiveTimeOut != null
                    && !effectiveTimeOut.equals(LocalTime.MIN)
                    && effectiveTimeOut.isBefore(effectiveTimeIn)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clock-out time cannot be before clock-in time");
            }
        }

        if (attendance.getTimeIn() != null && attendance.getTimeOut() != null && !attendance.getTimeOut().equals(LocalTime.MIN)) {
            Employee employee = attendance.getEmployee();

            if ( employee.getStartShift() == null
                    || employee.getEndShift() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Employee shift times are not configured. Cannot calculate hours.");
            }

            LocalTime employeeStartShift = employee.getStartShift();
            LocalTime employeeEndShift = employee.getEndShift();

            Duration duration = Duration.between(attendance.getTimeIn(), attendance.getTimeOut());
            if (duration.isNegative()) {
                duration = duration.plusHours(24);
            }
            BigDecimal totalHours = BigDecimal.valueOf(duration.toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            // Calculate regular hours based on employee's shift duration
            BigDecimal regularHours = BigDecimal
                    .valueOf(Duration.between(employeeStartShift, employeeEndShift).toMinutes())
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);

            BigDecimal overtime = totalHours.subtract(regularHours);
            if (overtime.compareTo(BigDecimal.ZERO) < 0) {
                overtime = BigDecimal.ZERO;
            }

            attendance.setTotalHours(totalHours);
            attendance.setOvertime(overtime);
        }

        return repository.save(attendance);
    }

    public Page<Attendance> getAllAttendances(int page, int limit, LocalDate startDate, LocalDate endDate) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, limit, sort);

        DateRange dateRange = new DateRange(startDate, endDate);
        return repository.findAllByDateBetween(dateRange.startDate(), dateRange.endDate(), pageable);
    }

    public Page<Attendance> getEmployeeAttendances(int page, int limit, Long employeeId, LocalDate startDate, LocalDate endDate) {
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

        Pageable pageable = PageRequest.of(page, limit);
        DateRange dateRange = new DateRange(startDate, endDate);

        return repository.findByEmployee_IdAndDateBetween(employeeId, dateRange.startDate(), dateRange.endDate(), pageable);
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
