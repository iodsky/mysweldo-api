package com.iodsky.sweldox.leave.credit;

import com.iodsky.sweldox.employee.EmployeeService;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.leave.LeaveType;
import com.iodsky.sweldox.security.user.User;
import com.iodsky.sweldox.security.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeaveCreditService {

    private final LeaveCreditRepository leaveCreditRepository;
    private final EmployeeService employeeService;
    private final UserService userService;

    private static final double DEFAULT_VACATION_CREDITS = 14.0;
    private static final double DEFAULT_SICK_CREDITS = 7.0;
    private static final double DEFAULT_BEREAVEMENT_CREDITS = 5.0;

    @Transactional
    public List<LeaveCredit> createLeaveCredits(LeaveCreditRequest dto) {
        Employee employee = employeeService.getEmployeeById(dto.getEmployeeId());

        boolean exists = leaveCreditRepository.existsByEmployee_IdAndEffectiveDate(employee.getId(), dto.getEffectiveDate());
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

        return leaveCreditRepository.saveAll(leaveCredits);
    }

    public LeaveCredit getLeaveCreditByEmployeeIdAndType(Long employeeId, LeaveType type) {
        return leaveCreditRepository.findByEmployee_IdAndType(employeeId, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No " + type + " leave credits found for employeeId: " + employeeId));
    }

    public List<LeaveCredit> getLeaveCreditsByEmployeeId() {
        User user = userService.getAuthenticatedUser();

        Long employeeId = user.getEmployee().getId();
        return leaveCreditRepository.findAllByEmployee_Id(employeeId);
    }

    public LeaveCredit updateLeaveCredit (UUID targetId, LeaveCredit updated) {
        LeaveCredit existing = leaveCreditRepository.findById(targetId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave credit not found: " + targetId));

        existing.setCredits(updated.getCredits());

        return leaveCreditRepository.save(existing);
    }

    public void deleteLeaveCreditsByEmployeeId(Long employeeId) {
        List<LeaveCredit> credits = leaveCreditRepository.findAllByEmployee_Id(employeeId);
        leaveCreditRepository.deleteAll(credits);
    }

}
