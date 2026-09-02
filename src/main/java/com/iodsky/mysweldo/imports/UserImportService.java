package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.role.RoleRepository;
import com.iodsky.mysweldo.security.user.User;
import com.iodsky.mysweldo.security.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class UserImportService extends AbstractImportService<UserImportRecord> {

    private final EmployeeService employeeService;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private Map<String, Role> roleCache;

    public UserImportService(ImportJobRepository importJobRepository,
                             ImportJobErrorRepository importJobErrorRepository,
                             EmployeeService employeeService,
                             RoleRepository roleRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder) {
        super(importJobRepository, importJobErrorRepository);
        this.employeeService = employeeService;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected Class<UserImportRecord> getRecordType() {
        return UserImportRecord.class;
    }

    @Override
    protected void importRecord(UserImportRecord record) {
        initializeRoleCache();

        long employeeId = Long.parseLong(record.getEmployeeId());
        Employee employee = employeeService.getEmployeeById(employeeId);

        Role role = roleCache.get(record.getRole());
        if (role == null) {
            throw new IllegalArgumentException("Invalid user role: " + record.getRole());
        }

        User user = User.builder()
                .employee(employee)
                .email(record.getEmail())
                .role(role)
                .password(passwordEncoder.encode(record.getPassword()))
                .build();

        userRepository.save(user);
    }

    private void initializeRoleCache() {
        if (roleCache == null) {
            log.info("Initializing user import role cache...");

            roleCache = new HashMap<>();
            List<Role> roles = roleRepository.findAll();
            for (Role role : roles) {
                roleCache.put(role.getName(), role);
            }

            log.info("Loaded {} user roles into cache", roles.size());
        }
    }

    @Override
    protected String reasonFor(Throwable t, UserImportRecord record) {
        if (t instanceof DataIntegrityViolationException) {
            String message = t.getMessage();
            if (message != null) {
                if (message.contains("email")) {
                    return duplicateReason("email", record.getEmail());
                } else if (message.contains("employee_id")) {
                    return "Employee not found or invalid employee reference: " + record.getEmployeeId();
                }
                return "Duplicate constraint violation";
            }
        }
        return t.getMessage();
    }

}