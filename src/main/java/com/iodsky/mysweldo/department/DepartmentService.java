package com.iodsky.mysweldo.department;

import com.iodsky.mysweldo.position.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository repository;
    private final PositionRepository positionRepository;

    public Department createDepartment(DepartmentRequest request) {
        if (repository.existsById(request.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Department with ID " + request.getId() + " already exists");
        }

        Department department = Department.builder()
                .id(request.getId())
                .title(request.getTitle())
                .build();

        return repository.save(department);
    }

    public Page<Department> getAllDepartments(int pageNo, int limit) {
        Pageable pageable = PageRequest.of(pageNo, limit, Sort.by("title").ascending());
        return repository.findAll(pageable);
    }

    public Department getDepartmentById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Department " + id + " not found"));
    }

    public Department updateDepartment(String id, DepartmentUpdateRequest request) {
        Department department = getDepartmentById(id);
        department.setTitle(request.getTitle());
        return repository.save(department);
    }

    public void deleteDepartment(String id) {
        Department department = getDepartmentById(id);

        // Check if department has active employees
        long employeeCount = repository.countEmployeesByDepartmentId(id);
        if (employeeCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete department '" + department.getTitle() + "'. It has " + employeeCount + " active employee(s) assigned to it."
            );
        }

        // Check if department has active positions
        if (positionRepository.existsByDepartmentId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete department '" + department.getTitle() + "'. It has active position(s) linked to it."
            );
        }

        // Soft delete
        department.setDeletedAt(Instant.now());
        repository.save(department);
    }

}
