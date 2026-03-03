package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentService;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionService;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitService;
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

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMapper employeeMapper;
    private final EmployeeRepository employeeRepository;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final BenefitService benefitService;

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

            List<Benefit> benefits = employee.getBenefits();

            benefits.forEach(b -> {
                b.setBenefitType(benefitService.getBenefitTypeById(b.getBenefitType().getId()));
            });

            return employeeRepository.save(employee);
    }

    public Page<Employee> getAllEmployees(int page, int limit, String departmentId, Long supervisorId, String status) {

        Pageable pageable = PageRequest.of(page, limit);

        if (departmentId != null) {
            return employeeRepository.findAllByDepartment_Id(departmentId, pageable);
        } else if (supervisorId != null) {
            return employeeRepository.findAllBySupervisor_Id(supervisorId, pageable);
        } else if (status != null) {
            return  employeeRepository.findAllByStatus(Status.valueOf(status.toUpperCase()), pageable);
        }

        return employeeRepository.findAll(pageable);
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
    public void deleteEmployeeById(Long id, Status finalStatus) {
        Employee employee = getEmployeeById(id);
        if (employee.isDeleted()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Employee already deleted");
        }

        if (finalStatus != Status.TERMINATED && finalStatus != Status.RESIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Final status must be TERMINATED or RESIGNED");
        }

        employeeRepository.findAllBySupervisor_Id(id).forEach(sub -> {
            sub.setSupervisor(null);
        });

        employee.setStatus(finalStatus);
        employee.setDeletedAt(Instant.now());
        employeeRepository.save(employee);
    }

    public List<Long> getAllActiveEmployeeIds() {
        return employeeRepository.findAllActiveEmployeeIds();
    }
    
}
