package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.leave.LeaveType;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveCreditService {

    private final LeaveCreditRepository repository;
    private final EmployeeService employeeService;
    private final UserService userService;

    private static final double DEFAULT_VACATION_CREDITS = 14.0;
    private static final double DEFAULT_SICK_CREDITS = 7.0;
    private static final double DEFAULT_BEREAVEMENT_CREDITS = 5.0;

    @Transactional
    public List<LeaveCredit> createLeaveCredits(LeaveCreditRequest dto) {
        Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());

        boolean exists = repository.existsByEmployee_IdAndEffectiveDate(employee.getId(), dto.getEffectiveDate());
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Leave credits already exists for employee " + employee.getId());
        }

        List<LeaveCredit> leaveCredits = List.of(
                LeaveCredit.builder()
                        .employee(employee)
                        .type(LeaveType.VACATION)
                        .credits(DEFAULT_VACATION_CREDITS)
                        .effectiveDate(dto.getEffectiveDate())
                        .build(),
                LeaveCredit.builder()
                        .employee(employee)
                        .type(LeaveType.SICK)
                        .credits(DEFAULT_SICK_CREDITS)
                        .effectiveDate(dto.getEffectiveDate())
                        .build(),
                LeaveCredit.builder()
                        .employee(employee)
                        .type(LeaveType.BEREAVEMENT)
                        .credits(DEFAULT_BEREAVEMENT_CREDITS)
                        .effectiveDate(dto.getEffectiveDate())
                        .build()
        );

        return repository.saveAll(leaveCredits);
    }

    public LeaveCredit getLeaveCreditByEmployeeIdAndType(Long employeeId, LeaveType type) {
        return repository.findByEmployee_IdAndType(employeeId, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No " + type + " leave credits found for employeeId: " + employeeId));
    }

    public List<LeaveCredit> getLeaveCreditsByEmployeeId() {
        User user = userService.getAuthenticatedUser();

        Long employeeId = user.getEmployee().getId();
        return repository.findAllByEmployee_Id(employeeId);
    }

    public LeaveCredit updateLeaveCredit (UUID targetId, LeaveCredit updated) {
        LeaveCredit existing = repository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave credit not found: " + targetId));

        existing.setCredits(updated.getCredits());

        return repository.save(existing);
    }

    public void deleteLeaveCreditsByEmployeeId(Long employeeId) {
        List<LeaveCredit> credits = repository.findAllByEmployee_Id(employeeId);
        Instant now = Instant.now();
        credits.forEach(credit -> credit.setDeletedAt(now));
        repository.saveAll(credits);
    }

}
