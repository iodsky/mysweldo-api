package com.iodsky.mysweldo.security.user;

import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.security.role.Role;
import com.iodsky.mysweldo.security.role.RoleService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository repository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final EmployeeService employeeService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + username + " not found"));
    }

    @Transactional
    public User createUser(UserRequest userRequest) {
        User user = userMapper.toEntity(userRequest);

        Employee employee = employeeService.getEmployeeById(userRequest.getEmployeeId());
        user.setEmployee(employee);

        Role role = getUserRole(userRequest.getRole());
        user.setRole(role);

        user.setPassword(passwordEncoder.encode(userRequest.getPassword()));

        return repository.save(user);
    }

    public Page<User> getAllUsers(int size, int limit, String roleName) {
        Pageable pageable = PageRequest.of(size, limit);
        if (roleName == null) {
            return repository.findAll(pageable);
        }

        return repository.findAllByRole_Name(roleName, pageable);
    }

    public User getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof User user) {
            return user;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No authenticated user found");
    }

    public User getUserByEmail(String email) {
        return repository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + email + " not found"));
    }

    @Transactional
    public User updateUserRole(UUID userId, String role) {
        User user = getUserById(userId);
        Role userRole = getUserRole(role);

        user.setRole(userRole);
        return repository.save(user);
    }

    public User getUserById(UUID id) {
       return repository.findById(id)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User " + id + " not found"));
    }

    private Role getUserRole(String roleName) {
        return roleService.getRoleByName(roleName);
    }

}
