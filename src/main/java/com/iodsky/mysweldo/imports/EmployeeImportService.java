package com.iodsky.mysweldo.imports;

import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitRepository;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.department.DepartmentRepository;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.EmployeeBenefit;
import com.iodsky.mysweldo.employee.EmployeeRepository;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.position.PositionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmployeeImportService extends AbstractImportService<EmployeeImportRecord> {

    private final PositionRepository positionRepository;
    private final DepartmentRepository departmentRepository;
    private final BenefitRepository benefitRepository;
    private final EmployeeRepository employeeRepository;

    private Map<String, Position> positionCache;
    private Map<String, Department> departmentCache;
    private Map<String, Benefit> benefitCache;

    public EmployeeImportService(ImportJobRepository importJobRepository,
                                 ImportJobErrorRepository importJobErrorRepository,
                                 PositionRepository positionRepository,
                                 DepartmentRepository departmentRepository,
                                 BenefitRepository benefitRepository,
                                 EmployeeRepository employeeRepository) {
        super(importJobRepository, importJobErrorRepository);
        this.positionRepository = positionRepository;
        this.departmentRepository = departmentRepository;
        this.benefitRepository = benefitRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected Class<EmployeeImportRecord> getRecordType() {
        return EmployeeImportRecord.class;
    }

    @Override
    protected void importRecord(EmployeeImportRecord record) {
        initializeCaches();

        Employee employee = EmployeeImportRecord.toEntity(record);

        applyPosition(record, employee);
        applyBenefits(record, employee);

        employeeRepository.save(employee);
    }

    private void applyPosition(EmployeeImportRecord item, Employee entity) {
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

    private void applyBenefits(EmployeeImportRecord item, Employee entity) {
        List<EmployeeBenefit> benefits = new ArrayList<>();

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

    private void addBenefit(List<EmployeeBenefit> benefits, Employee employee, String benefitCode,
                            String amountStr, EmployeeImportRecord csvRow) {
        try {
            Benefit benefit = benefitCache.get(benefitCode.toUpperCase());

            if (benefit == null) {
                log.warn("Benefit type '{}' not found for employee {} {}. Skipping this benefit.",
                        benefitCode, csvRow.getFirstName(), csvRow.getLastName());
                return;
            }

            BigDecimal amount = new BigDecimal(amountStr);

            EmployeeBenefit employeeBenefit = EmployeeBenefit.builder()
                    .employee(employee)
                    .benefit(benefit)
                    .amount(amount)
                    .build();

            benefits.add(employeeBenefit);
        } catch (NumberFormatException e) {
            log.warn("Invalid amount '{}' for benefit '{}' for employee {} {}. Skipping this benefit.",
                    amountStr, benefitCode, csvRow.getFirstName(), csvRow.getLastName());
        }
    }

    private void initializeCaches() {
        if (positionCache == null) {
            log.info("Initializing employee import reference data caches...");

            positionCache = new HashMap<>();
            for (Position position : positionRepository.findAll()) {
                positionCache.put(position.getTitle().toUpperCase(), position);
            }

            departmentCache = new HashMap<>();
            for (Department department : departmentRepository.findAll()) {
                departmentCache.put(department.getTitle().toUpperCase(), department);
            }

            benefitCache = new HashMap<>();
            for (Benefit benefit : benefitRepository.findAll()) {
                benefitCache.put(benefit.getCode().toUpperCase(), benefit);
            }

            log.info("Employee import reference data caches initialized");
        }
    }

    @Override
    protected String reasonFor(Throwable t, EmployeeImportRecord record) {
        if (t instanceof DataIntegrityViolationException) {
            String message = t.getMessage();
            if (message != null) {
                if (message.contains("sss_no")) {
                    return duplicateReason("SSS Number", record.getSssNumber());
                } else if (message.contains("tin_no")) {
                    return duplicateReason("TIN Number", record.getTinNumber());
                } else if (message.contains("philhealth_no")) {
                    return duplicateReason("PhilHealth Number", record.getPhilhealthNumber());
                } else if (message.contains("pagibig_no")) {
                    return duplicateReason("PagIbig Number", record.getPagIbigNumber());
                } else if (message.contains("phone_number")) {
                    return duplicateReason("Phone Number", record.getPhoneNumber());
                } else if (message.contains("address")) {
                    return duplicateReason("Address", record.getAddress());
                }
                return "Duplicate constraint violation";
            }
        }
        return t.getMessage();
    }

}