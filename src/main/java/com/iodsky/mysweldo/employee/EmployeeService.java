package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.benefit.BenefitService;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentService;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionService;
import com.iodsky.mysweldo.security.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final BenefitService benefitService;

    public static final List<EmploymentStatus> NON_ACTIVE_STATUSES = List.of(EmploymentStatus.RESIGNED, EmploymentStatus.TERMINATED);

    @Transactional
    public Employee createEmployee(EmployeeRequest request) {
            Employee employee = employeeMapper.toEntity(request);

            Employee supervisor = null;
            if (request.getSupervisorId() != null) {
                supervisor = getEmployeeById(request.getSupervisorId());
            }

            Department department = departmentService.getDepartmentById(request.getDepartmentId());
            Position position = positionService.getPositionById(request.getPositionId());

            employee.setSupervisor(supervisor);
            employee.setDepartment(department);
            employee.setPosition(position);

            List<EmployeeBenefit> benefits = employee.getBenefits();

            benefits.forEach(b -> {
                b.setBenefit(benefitService.getBenefitByCode(b.getBenefit().getCode()));
            });

            return employeeRepository.save(employee);
    }

    public Page<EmployeeBasicDto> getAllEmployees(int page, int limit, String departmentId, Long supervisorId, String status) {
        Pageable pageable = PageRequest.of(page, limit);
        Page<EmployeeBasic> result;

        if (departmentId != null) {
            result =  employeeRepository.findAllByDepartment_Id(departmentId, pageable);
        } else if (supervisorId != null) {
            result = employeeRepository.findAllBySupervisor_Id(supervisorId, pageable);
        } else if (status != null) {
            result = employeeRepository.findAllByStatus(EmploymentStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            result = employeeRepository.findAllByStatusNotIn(NON_ACTIVE_STATUSES, pageable);
        }

        return result.map(employeeMapper::toBasicDto);
    }

    public Employee getAuthenticatedEmployee() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User user) {
            return user.getEmployee();
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found");
    }

    public Employee getEmployeeById(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee " + id + " not found"));
    }

    public Employee updateEmployeeById(Long id, EmployeeRequest request) {
        Employee employee = this.getEmployeeById(id);

            Employee supervisor = null;
            if (request.getSupervisorId() != null) {
                supervisor = getEmployeeById(request.getSupervisorId());
            }

            Department department = departmentService.getDepartmentById(request.getDepartmentId());
            Position position = positionService.getPositionById(request.getPositionId());

            employee.setSupervisor(supervisor);
            employee.setDepartment(department);
            employee.setPosition(position);

            employeeMapper.updateEntity(employee, request);

            return employeeRepository.save(employee);

    }

    @Transactional
    public void updateEmployeeStatus(Long id, EmploymentStatus finalStatus) {
        Employee employee = getEmployeeById(id);
        EmploymentStatus currentStatus = employee.getStatus();
        if (currentStatus == EmploymentStatus.TERMINATED || currentStatus == EmploymentStatus.RESIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee already " + currentStatus);
        }

        if (finalStatus != EmploymentStatus.TERMINATED && finalStatus != EmploymentStatus.RESIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Final status must be TERMINATED or RESIGNED");
        }

        employeeRepository.findAllBySupervisor_Id(id).forEach(sub -> {
            sub.setSupervisor(null);
        });

        employee.setStatus(finalStatus);
        employeeRepository.save(employee);
    }

    public List<Long> getAllActiveEmployeeIds() {
        return employeeRepository.findAllActiveEmployeeIds();
    }

    public Page<EmployeeBasic> getEmployees(Pageable pageable) {
        return employeeRepository.findAllByStatusNotIn(NON_ACTIVE_STATUSES, pageable);
    }

}
