package com.iodsky.sweldox.overtime;

import com.iodsky.sweldox.attendance.Attendance;
import com.iodsky.sweldox.attendance.AttendanceService;
import com.iodsky.sweldox.common.DateRange;
import com.iodsky.sweldox.common.RequestStatus;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.employee.EmployeeService;
import com.iodsky.sweldox.security.user.User;
import com.iodsky.sweldox.security.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OvertimeRequestService {

    private final EmployeeService employeeService;
    private final UserService userService;
    private final AttendanceService attendanceService;
    private final OvertimeRequestRepository repository;

    @Transactional
    public OvertimeRequest createOvertimeRequest(AddOvertimeRequest request) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isHR = authenticatedUser.getUserRole().getRole().equals("HR");
        Long employeeId = request.getEmployeeId();

        if (employeeId == null) {
            employeeId = authenticatedUser.getEmployee().getId();
        } else if (!isHR) {
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have the permissions to access this resource");
        }

        if (repository.existsByEmployee_IdAndDate(employeeId, request.getDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Overtime request already exists for employee id: " + employeeId + " date: " + request.getDate());
        }

        Employee employee = employeeService.getEmployeeById(employeeId);
        Attendance attendance = getEmployeeAttendanceByDate(employeeId, request.getDate());

        OvertimeRequest overtimeRequest = OvertimeRequest.builder()
                .employee(employee)
                .date(request.getDate())
                .overtimeHours(attendance.getOvertime())
                .status(RequestStatus.PENDING)
                .reason(request.getReason())
                .build();

        return repository.save(overtimeRequest);
    }

    public Page<OvertimeRequest> getOvertimeRequests(LocalDate startDate, LocalDate endDate, int page, int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable  = PageRequest.of(page, limit, sort);

        if (startDate != null || endDate != null) {
            DateRange dateRange = new DateRange(startDate, endDate);
            return repository.findByDateBetween(dateRange.startDate(), dateRange.endDate(), pageable);
        }

        return repository.findAll(pageable);
    }

    public Page<OvertimeRequest> getEmployeeOvertimeRequest(int page, int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable  = PageRequest.of(page, limit, sort);

        Long employeeId = userService.getAuthenticatedUser().getEmployee().getId();

        return repository.findAllByEmployee_Id(employeeId, pageable);
    }


    public OvertimeRequest getOvertimeRequestById(UUID id) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isHR = authenticatedUser.getUserRole().getRole().equals("HR");
        Long employeeId =authenticatedUser.getEmployee().getId();

        OvertimeRequest overtimeRequest = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Overtime request not found for id: " + id));

        if (!overtimeRequest.getEmployee().getId().equals(employeeId) && !isHR) {
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have the permissions to access this resource");
        }

        return overtimeRequest;
    }

    @Transactional
    public OvertimeRequest updateOvertimeRequest(UUID id, UpdateOvertimeRequest request) {
        OvertimeRequest existing = getOvertimeRequestById(id);

        if (existing.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update overtime request with status: " + existing.getStatus());
        }

        if (!existing.getDate().equals(request.getDate())) {

            if (repository.existsByEmployee_IdAndDate(existing.getEmployee().getId(), request.getDate())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Overtime request already exists for date: " + request.getDate());
            }

            Attendance attendance = getEmployeeAttendanceByDate(existing.getEmployee().getId(), request.getDate());
            
            existing.setDate(request.getDate());
            existing.setOvertimeHours(attendance.getOvertime());
        }

        existing.setReason(request.getReason());
        return repository.save(existing);
    }

    public OvertimeRequest updateOvertimeRequestStatus(UUID id, RequestStatus status) {
        OvertimeRequest existing = getOvertimeRequestById(id);
        existing.setStatus(status);
        return repository.save(existing);
    }

    public void deleteOvertimeRequest(UUID id) {
        OvertimeRequest existing = getOvertimeRequestById(id);

        if (!existing.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot delete approved or rejected overtime request");
        }

        existing.setDeletedAt(Instant.now());
        repository.save(existing);
    }

    private Attendance getEmployeeAttendanceByDate(Long employeeId, LocalDate date) {
        Attendance attendance = attendanceService.getEmployeeAttendanceByDate(employeeId, date);

        if (attendance == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No attendance found for employee id: " + employeeId + " date: " + date);
        }

        if (attendance.getOvertime().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No overtime hours for given date " + date);
        }

        return  attendance;
    }

}
