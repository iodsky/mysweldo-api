package com.iodsky.mysweldo.security.role;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository repository;

    public Role createRole(RoleRequest request) {
        String name = request.getName();
        if (repository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role " + name + " already exists");
        }

        Role role = Role.builder()
                .name(name)
                .description(request.getDescription())
                .build();

        return repository.save(role);
    }

    public Role getRoleById(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found for id: " + id));
    }

    public Role getRoleByName(String name) {
        return repository.findByName(name).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role " + name + " not found"));
    }

    public Page<Role> getAllRoles(int pageNo, int limit) {
        Pageable pageable = PageRequest.of(pageNo, limit);

        return repository.findAll(pageable);
    }

    public Role updateRole(Long id, RoleRequest request) {
        Role role = getRoleById(id);

        if (request.getName() != null) role.setName(request.getName());
        if (request.getDescription() != null) role.setDescription(request.getDescription());

        return repository.save(role);
    }

    public void deleteRole(Long id) {
        Role role = getRoleById(id);

        if (repository.isRoleUsedById(role.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Role " + id + " is still used");

        role.setDeletedAt(Instant.now());
        repository.save(role);
    }

}
