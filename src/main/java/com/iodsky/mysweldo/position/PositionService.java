package com.iodsky.mysweldo.position;

import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentService;
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
public class PositionService {

    private final PositionRepository repository;
    private final DepartmentService departmentService;
    private final PositionMapper mapper;

    public Position createPosition(PositionRequest request) {
        if (repository.existsById(request.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Position with ID " + request.getId() + " already exists");
        }

        Department department = departmentService.getDepartmentById(request.getDepartmentId());

        Position position = mapper.toEntity(request);
        position.setDepartment(department);

        return repository.save(position);
    }

    public Page<Position> getAllPositions(int pageNo, int limit) {
        Pageable pageable = PageRequest.of(pageNo, limit, Sort.by("title").ascending());
        return repository.findAll(pageable);
    }

    public Position getPositionById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Position " + id + " not found"));
    }

    public Position updatePosition(String id, PositionUpdateRequest request) {
        Position position = getPositionById(id);
        Department department = departmentService.getDepartmentById(request.getDepartmentId());

        if (!position.getDepartment().getId().equals(request.getDepartmentId())) {
            long employeeCount = repository.countEmployeesByPositionId(id);
            if (employeeCount > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Cannot change department for position '" + position.getTitle() + "'. It has " + employeeCount + " active employee(s) assigned to it."
                );
            }
        }

        position.setDepartment(department);
        position.setTitle(request.getTitle());

        return repository.save(position);
    }

    public void deletePosition(String id) {
        Position position = getPositionById(id);

        // Check if position has active employees
        long employeeCount = repository.countEmployeesByPositionId(id);
        if (employeeCount > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete position '" + position.getTitle() + "'. It has " + employeeCount + " active employee(s) assigned to it."
            );
        }
        // Soft delete
        position.setDeletedAt(Instant.now());
        repository.save(position);
    }

    public boolean hasPositionsByDepartmentId(String departmentId) {
        return repository.existsByDepartmentId(departmentId);
    }

}
