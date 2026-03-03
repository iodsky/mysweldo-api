package com.iodsky.mysweldo.batch.employee;

import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeRepository;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentRepository;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionRepository;
import com.iodsky.mysweldo.benefit.Benefit;

import com.iodsky.mysweldo.benefit.BenefitType;
import com.iodsky.mysweldo.benefit.BenefitTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

/**
 * ItemProcessor for transforming EmployeeImportRecord to Employee entity with validation.
 * Uses in-memory caching to optimize database lookups for reference data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeImportProcessor implements ItemProcessor<EmployeeImportRecord, Employee> {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final BenefitTypeRepository benefitTypeRepository;
    private final EmployeeRepository employeeRepository;

    // Cache for reference data to avoid repeated database queries
    private Map<String, Position> positionCache;
    private Map<String, Department> departmentCache;
    private Map<String, BenefitType> benefitTypeCache;
    private Map<Long, Employee> supervisorCache;

    @Override
    public Employee process(EmployeeImportRecord item) {
        log.debug("Processing employee: {} {}", item.getFirstName(), item.getLastName());

        // Initialize caches on first run (lazy loading)
        initializeCaches();

        // Convert CSV record to Employee entity
        Employee employee = EmployeeImportRecord.toEntity(item);

        // Validate and set Position
        validateEmployeePosition(item, employee);

        // Validate and set Supervisor
        validateEmployeeSupervisor(item, employee);

        // Validate and set Benefits
        validateEmployeeBenefits(item, employee);

        log.debug("Successfully processed employee: {} {}", item.getFirstName(), item.getLastName());
        return employee;
    }

    private void validateEmployeePosition(EmployeeImportRecord item, Employee entity) {
        if (item.getPosition() != null && !item.getPosition().isEmpty()) {
            Position position = positionCache.get(item.getPosition().toUpperCase());
            if (position == null) {
                log.warn("Position '{}' not found for employee {} {}. Setting position and department to null.",
                        item.getPosition(), item.getFirstName(), item.getLastName());
                entity.setPosition(null);
                entity.setDepartment(null);
                return;
            }
            entity.setPosition(position);

            if (position.getDepartment() != null) {
                entity.setDepartment(position.getDepartment());
            }
        }
    }

    private void validateEmployeeSupervisor(EmployeeImportRecord item, Employee entity) {
        if (item.getSupervisorId() != null && !item.getSupervisorId().isEmpty()) {
            try {
                Long supervisorId = Long.parseLong(item.getSupervisorId());
                Employee supervisor = supervisorCache.get(supervisorId);

                if (supervisor == null) {
                    supervisor = employeeRepository.findById(supervisorId).orElse(null);
                    if (supervisor != null) {
                        supervisorCache.put(supervisorId, supervisor);
                    } else {
                        log.warn("Supervisor with ID '{}' not found for employee {} {}. Setting supervisor to null.",
                                supervisorId, item.getFirstName(), item.getLastName());
                    }
                }
                entity.setSupervisor(supervisor);
            } catch (NumberFormatException e) {
                log.warn("Invalid supervisor ID '{}' for employee {} {}.",
                        item.getSupervisorId(), item.getFirstName(), item.getLastName());
            }
        }
    }


    private void validateEmployeeBenefits(EmployeeImportRecord item, Employee entity) {
        List<Benefit> benefits = new ArrayList<>();

        if (item.getMealAllowance() != null && !item.getMealAllowance().isEmpty()) {
            addBenefit(benefits, entity, "MEAL", item.getMealAllowance(), item);
        }

        if (item.getPhoneAllowance() != null && !item.getPhoneAllowance().isEmpty()) {
            addBenefit(benefits, entity, "PHONE", item.getPhoneAllowance(), item);
        }

        if (item.getClothingAllowance() != null && !item.getClothingAllowance().isEmpty()) {
            addBenefit(benefits, entity, "CLOTHING", item.getClothingAllowance(), item);
        }

        entity.setBenefits(benefits);
    }

    /**
     * Initialize caches with reference data from database.
     * This is called once before processing the first item.
     */
    private void initializeCaches() {
        if (positionCache == null) {
            log.info("Initializing reference data caches...");

            // Load all positions into cache
            positionCache = new HashMap<>();
            List<Position> positions = positionRepository.findAll();
            for (Position position : positions) {
                positionCache.put(position.getTitle().toUpperCase(), position);
            }
            log.info("Loaded {} positions into cache", positions.size());

            // Load all departments into cache
            departmentCache = new HashMap<>();
            List<Department> departments = departmentRepository.findAll();
            for (Department department : departments) {
                departmentCache.put(department.getTitle().toUpperCase(), department);
            }
            log.info("Loaded {} departments into cache", departments.size());

            // Load all benefit types into cache
            benefitTypeCache = new HashMap<>();
            List<BenefitType> benefitTypes = benefitTypeRepository.findAll();
            for (BenefitType benefitType : benefitTypes) {
                benefitTypeCache.put(benefitType.getId().toUpperCase(), benefitType);
            }
            log.info("Loaded {} benefit types into cache", benefitTypes.size());

            // Initialize supervisor cache (will be populated on-demand)
            supervisorCache = new HashMap<>();

            log.info("Reference data caches initialized successfully");
        }
    }

    /**
     * Helper method to add a benefit with validation.
     */
    private void addBenefit(List<Benefit> benefits, Employee employee, String benefitTypeId,
                           String amountStr, EmployeeImportRecord csvRow) {
        try {
            BenefitType benefitType = benefitTypeCache.get(benefitTypeId.toUpperCase());

            if (benefitType == null) {
                log.warn("Benefit type '{}' not found for employee {} {}. Skipping this benefit.",
                        benefitTypeId, csvRow.getFirstName(), csvRow.getLastName());
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);

            Benefit benefit = Benefit.builder()
                    .employee(employee)
                    .benefitType(benefitType)
                    .amount(amount)
                    .build();

            benefits.add(benefit);
        } catch (NumberFormatException e) {
            log.warn("Invalid amount '{}' for benefit type '{}' for employee {} {}. Skipping this benefit.",
                    amountStr, benefitTypeId, csvRow.getFirstName(), csvRow.getLastName());
        }
    }


    public void clearCaches() {
        if (positionCache != null) positionCache.clear();
        if (departmentCache != null) departmentCache.clear();
        if (benefitTypeCache != null) benefitTypeCache.clear();
        if (supervisorCache != null) supervisorCache.clear();

        positionCache = null;
        departmentCache = null;
        benefitTypeCache = null;
        supervisorCache = null;

        log.info("Caches cleared");
    }

}
