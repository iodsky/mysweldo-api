package com.iodsky.mysweldo.overtime;

import com.iodsky.mysweldo.common.DateRange;
import com.iodsky.mysweldo.common.RequestStatus;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
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
    private final OvertimeRequestRepository repository;

    @Transactional
    public OvertimeRequest createOvertimeRequest(OvertimeRequestDto request) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isHR = authenticatedUser.getRole().getName().equals("HR");
        Long employeeId = request.getEmployeeId();

        if (employeeId == null) {
            employeeId = authenticatedUser.getEmployee().getId();
        } else if (!isHR) {
            throw  new ResponseStatusException(HttpStatus.FORBIDDEN,"You don't have the permissions to access this resource");
        }

        if (repository.existsByEmployee_IdAndDate(employeeId, request.getDate())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Overtime request already exists for employee id: " + employeeId + " date: " + request.getDate());
        }

        Employee employee = employeeService.getEmployeeById(employeeId);

        OvertimeRequest overtimeRequest = OvertimeRequest.builder()
                .employee(employee)
                .date(request.getDate())
                .overtimeHours(request.getOvertimeHours())
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

    public Page<OvertimeRequest> getSubordinatesOvertimeRequests(int page, int limit) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Long supervisorId = authenticatedUser.getEmployee().getId();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable  = PageRequest.of(page, limit, sort);

        return repository.findByEmployee_Supervisor_Id(supervisorId, pageable);
    }

    public OvertimeRequest getOvertimeRequestById(UUID id) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isHR = authenticatedUser.getRole().getName().equals("HR");
        Long employeeId = authenticatedUser.getEmployee().getId();

        OvertimeRequest request = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request " + id + " not found"));

        boolean isSupervisor = request.getEmployee().getSupervisor() != null &&
                request.getEmployee().getSupervisor().getId().equals(employeeId);

        if (!request.getEmployee().getId().equals(employeeId) && !isHR && !isSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to access this resource");
        }

        return request;
    }

    @Transactional
    public OvertimeRequest updateOvertimeRequest(UUID id, OvertimeRequestDto request) {
        User authenticatedUser = userService.getAuthenticatedUser();
        OvertimeRequest existing = getOvertimeRequestById(id);
        boolean isHr = authenticatedUser.getRole().getName().equals("HR");

        if (!existing.getEmployee().getId().equals(authenticatedUser.getEmployee().getId()) && !isHr) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have the permission to access this resource");
        }

        if (existing.getStatus() != RequestStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot update overtime request with status: " + existing.getStatus());
        }

        if (!existing.getDate().equals(request.getDate())) {

            if (repository.existsByEmployee_IdAndDate(existing.getEmployee().getId(), request.getDate())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Overtime request already exists for date: " + request.getDate());
            }
            
            existing.setDate(request.getDate());
            existing.setOvertimeHours(request.getOvertimeHours());
        }

        existing.setReason(request.getReason());
        return repository.save(existing);
    }

    @Transactional
    public OvertimeRequest updateOvertimeRequestStatus(UUID id, RequestStatus status) {
        User authenticatedUser = userService.getAuthenticatedUser();
        OvertimeRequest request = getOvertimeRequestById(id);

        boolean isHR = authenticatedUser.getRole().getName().equals("HR");

        boolean isSupervisor = request.getEmployee().getSupervisor() != null &&
                request.getEmployee().getSupervisor().getId().equals(authenticatedUser.getEmployee().getId());

        if (!isHR && !isSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to approve this request");
        }

        if (!request.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Overtime request " + id + " has already been processed");
        }

        request.setStatus(status);
        return repository.save(request);
    }

    public void deleteOvertimeRequest(UUID id) {
        User authenticatedUser = userService.getAuthenticatedUser();
        OvertimeRequest existing = getOvertimeRequestById(id);
        boolean isHr = authenticatedUser.getRole().getName().equals("HR");

        if (!existing.getEmployee().getId().equals(authenticatedUser.getEmployee().getId()) && !isHr) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have the permissions to access this resource");
        }

        if (!existing.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete approved or rejected overtime request");
        }

        existing.setDeletedAt(Instant.now());
        repository.save(existing);
    }

    public BigDecimal calculateApprovedOvertimeHours(Long employeeId, LocalDate startDate, LocalDate endDate) {
        return repository.sumOvertimeHoursByEmployeeI_IdAndDateBetweenAndStatus(employeeId, startDate, endDate, RequestStatus.APPROVED);
    }

}
