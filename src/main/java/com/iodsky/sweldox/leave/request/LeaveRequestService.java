package com.iodsky.sweldox.leave.request;

import com.iodsky.sweldox.common.RequestStatus;
import com.iodsky.sweldox.leave.LeaveType;
import com.iodsky.sweldox.leave.credit.LeaveCredit;
import com.iodsky.sweldox.leave.credit.LeaveCreditService;
import com.iodsky.sweldox.security.user.User;
import com.iodsky.sweldox.security.user.UserService;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaveRequestService {

    private final LeaveRequestRepository repository;
    private final LeaveCreditService leaveCreditService;
    private final LeaveRequestMapper mapper;
    private final UserService userService;

    @Transactional
    public LeaveRequest createLeaveRequest(LeaveRequestDto dto) {
        User user = userService.getAuthenticatedUser();
        Long employeeId = user.getEmployee().getId();

        LeaveType type = resolveLeaveType(dto.getLeaveType());

        // validate dates
        validateDates(employeeId, dto);

        // validate leave credits
        double daysRequired = calculateTotalDays(dto.getStartDate(), dto.getEndDate());
        LeaveCredit leaveCredit = leaveCreditService.getLeaveCreditByEmployeeIdAndType(employeeId, type);

        if (daysRequired > leaveCredit.getCredits()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("Insufficient leave credits. Required: %.1f days, Available: %.1f days",
                            daysRequired, leaveCredit.getCredits())
            );
        }

        LeaveRequest leave = LeaveRequest.builder()
                .employee(user.getEmployee())
                .leaveType(type)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .note(dto.getNote())
                .status(RequestStatus.PENDING)
                .build();

        return repository.save(leave);
    }

    public Page<LeaveRequest> getLeaveRequests(int pageNo, int limit) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable page = PageRequest.of(pageNo, limit, sort);

        return repository.findAll(page);
    }

    public Page<LeaveRequest> getEmployeeLeaveRequests(int pageNo, int limit) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Long employeeId = authenticatedUser.getEmployee().getId();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable page = PageRequest.of(pageNo, limit, sort);

        return repository.findAllByEmployee_Id(employeeId, page);
    }

    public Page<LeaveRequest> getSubordinatesLeaveRequests(int pageNo, int limit) {
        User authenticatedUser = userService.getAuthenticatedUser();
        Long supervisorId = authenticatedUser.getEmployee().getId();

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageNo, limit, sort);

        return repository.findAllByEmployee_Supervisor_Id(supervisorId, pageable);
    }

    public LeaveRequest getLeaveRequestById(String id) {
        User authenticatedUser = userService.getAuthenticatedUser();
        boolean isHR = authenticatedUser.getUserRole().getRole().equals("HR");
        Long employeeId = authenticatedUser.getEmployee().getId();

        LeaveRequest leaveRequest = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request " + id + " not found"));

        boolean isSupervisor = leaveRequest.getEmployee().getSupervisor() != null &&
                leaveRequest.getEmployee().getSupervisor().getId().equals(employeeId);

        if (!leaveRequest.getEmployee().getId().equals(employeeId) && !isHR && !isSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to access this resource");
        }

        return leaveRequest;
    }

    @Transactional
    public LeaveRequest updateLeaveRequest(String id, LeaveRequestDto dto) {
        User user = userService.getAuthenticatedUser();

        LeaveRequest entity = getLeaveRequestById(id);
        if (!entity.getEmployee().getId().equals(user.getEmployee().getId())) {
            if (!user.getUserRole().getRole().equals("HR")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this resource");
            }
        }

        if (!entity.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete processed leave request");
        }

        LeaveRequest updated = mapper.updateEntity(entity, dto);

        return repository.save(updated);
    }

    @Transactional
    public LeaveRequest updateLeaveStatus(String id, RequestStatus status) {
        User authenticatedUser = userService.getAuthenticatedUser();
        LeaveRequest leaveRequest = getLeaveRequestById(id);

        boolean isHR = authenticatedUser.getUserRole().getRole().equals("HR");

        boolean isSupervisor = leaveRequest.getEmployee().getSupervisor() != null &&
                leaveRequest.getEmployee().getSupervisor().getId().equals(authenticatedUser.getEmployee().getId());

        if (!isHR && !isSupervisor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You don't have permission to approve this request");
        }

        if (!leaveRequest.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave request " + id + " has already been processed");
        }

        leaveRequest.setStatus(status);

        if (status.equals(RequestStatus.APPROVED)) {
            double daysToDeduct = calculateTotalDays(leaveRequest.getStartDate(), leaveRequest.getEndDate());
            LeaveCredit leaveCredit = leaveCreditService.getLeaveCreditByEmployeeIdAndType(leaveRequest.getEmployee().getId(), leaveRequest.getLeaveType());

            if (leaveCredit.getCredits() < daysToDeduct) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("Cannot approve leave request. Insufficient credits. " +
                                "Required: %.1f days, Available: %.1f days. ",
                                daysToDeduct, leaveCredit.getCredits())
                );
            }

            double newCredits = leaveCredit.getCredits() - daysToDeduct;
            leaveCredit.setCredits(newCredits);

            leaveCreditService.updateLeaveCredit(leaveCredit.getId(), leaveCredit);
        }

        try {
            return repository.save(leaveRequest);
        } catch (OptimisticLockException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave credits were modified by another process.");
        }
    }

    public void deleteLeaveRequest(String id) {
        User user = userService.getAuthenticatedUser();

        LeaveRequest leaveRequest = getLeaveRequestById(id);
        if (!leaveRequest.getEmployee().getId().equals(user.getEmployee().getId())) {
            if (!user.getUserRole().getRole().equals("HR")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You don't have permission to access this resource");
            }
        }

        if (!leaveRequest.getStatus().equals(RequestStatus.PENDING)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete processed leave request");
        }

        repository.delete(leaveRequest);
    }

    private double calculateTotalDays(LocalDate startDate, LocalDate endDate) {
        double days = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (!isWeekend(date)) {
                days++;
            }
            date = date.plusDays(1);
        }

        return days;
    }

    private void validateDates(Long employeeId, LeaveRequestDto dto) {
        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date range");
        }

        LocalDate startDate = dto.getStartDate();
        if (isWeekend(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must be a weekday");
        }

        LocalDate endDate = dto.getEndDate();
        if (isWeekend(endDate)) {
            throw  new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date must be a weekday");
        }

        // Duplicate dates
        if (repository.existsByEmployee_IdAndStartDateAndEndDate(
                employeeId, dto.getStartDate(), dto.getEndDate()
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate leave request");
        }

        // Overlapping dates
        if (repository
                .existsByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employeeId,
                        List.of(RequestStatus.PENDING, RequestStatus.APPROVED),
                        endDate,
                        startDate
                )
        ) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Leave request overlaps with an existing pending or approved leave");
        }
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private LeaveType resolveLeaveType(String leaveTypeStr) {
        LeaveType type;
        try {
            type = LeaveType.valueOf(leaveTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid leave type: " + leaveTypeStr);
        }
        return type;
    }

}
