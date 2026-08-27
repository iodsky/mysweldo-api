package com.iodsky.mysweldo.leave.credit;

import com.iodsky.mysweldo.employee.EmployeeBasic;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.leave.LeaveType;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeaveCreditService {

    private final LeaveCreditRepository repository;
    private final EmployeeService employeeService;
    private final UserService userService;

    private static final double DEFAULT_VACATION_CREDITS = 14.0;
    private static final double DEFAULT_SICK_CREDITS = 7.0;
    private static final double DEFAULT_BEREAVEMENT_CREDITS = 5.0;

    public Page<EmployeeLeaveCreditDto> getAllLeaveCredits(int pageNo, int limit) {
        Pageable pageable = PageRequest.of(pageNo, limit);
        Page<EmployeeBasic> employeePage = employeeService.getEmployees(pageable);

        List<Long> employeeIds = employeePage.getContent()
                .stream().map(EmployeeBasic::getId).toList();

        Map<Long, List<LeaveCredit>> creditsByEmployee = repository
                .findAllByEmployee_IdIn(employeeIds)
                .stream()
                .collect(Collectors.groupingBy(lc -> lc.getEmployee().getId()));

        List<EmployeeLeaveCreditDto> dtos = employeePage.getContent().stream()
                .map(emp -> EmployeeLeaveCreditDto.builder()
                        .employeeId(emp.getId())
                        .firstName(emp.getFirstName())
                        .lastName(emp.getLastName())
                        .credits(creditsByEmployee.getOrDefault(emp.getId(), List.of())
                                .stream()
                                .map(lc -> CreditSummary.builder()
                                        .type(lc.getType().toString())
                                        .credits(lc.getCredits())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return new PageImpl<>(dtos, pageable, employeePage.getTotalElements());
    }

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

}
