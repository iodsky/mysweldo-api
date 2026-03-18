package com.iodsky.mysweldo.employee;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmployeeMapper  {

    private final EmployeeBenefitMapper benefitMapper;

    public EmployeeDto toDto(Employee employee) {

        Employee supervisor = employee.getSupervisor();
        String supervisorName = supervisor != null ? supervisor.getFirstName() + " " + supervisor.getLastName() : "N/A";

        List<EmployeeBenefitDto> benefits = employee.getBenefits()
                .stream()
                .map(b -> EmployeeBenefitDto.builder()
                        .benefit(b.getBenefit().getCode().toLowerCase())
                        .amount(b.getAmount())
                        .build())
                .toList();

        BigDecimal salary = employee.getSalary().getBaseAmount() != null ? employee.getSalary().getBaseAmount() : BigDecimal.ZERO;
        return EmployeeDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .birthday(employee.getBirthday())
                .address(employee.getAddress())
                .phoneNumber(employee.getPhoneNumber())
                .sssNumber(employee.getGovernmentId() == null ? null : employee.getGovernmentId().getSssNumber())
                .tinNumber((employee.getGovernmentId() == null ? null : employee.getGovernmentId().getTinNumber()))
                .philhealthNumber((employee.getGovernmentId() == null ? null : employee.getGovernmentId().getPhilhealthNumber()))
                .pagIbigNumber((employee.getGovernmentId() == null ? null : employee.getGovernmentId().getPagIbigNumber()))
                .status(employee.getStatus().toString())
                .type(employee.getType().toString())
                .supervisor(supervisorName)
                .department(employee.getDepartment().getTitle())
                .position(employee.getPosition().getTitle())
                .startShift(employee.getStartShift())
                .endShift(employee.getEndShift())
                .basicSalary(salary)
                .benefits(benefits)
                .build();
    }

    public Employee toEntity(EmployeeRequest request) {
        Employee employee = Employee.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthday(request.getBirthday())
                .address(request.getAddress())
                .phoneNumber(request.getPhoneNumber())
                .status(request.getStatus())
                .type(request.getType())
                .startShift(request.getStartShift())
                .endShift(request.getEndShift())
                .build();

        GovernmentId governmentId = GovernmentId.builder()
                .employee(employee)
                .sssNumber(request.getGovernmentId().getSssNumber())
                .tinNumber(request.getGovernmentId().getTinNumber())
                .philhealthNumber(request.getGovernmentId().getPhilhealthNumber())
                .pagIbigNumber(request.getGovernmentId().getPagIbigNumber())
                .build();

        Salary salary = Salary.builder()
                .employee(employee)
                .type(SalaryType.MONTHLY)
                .amount(request.getBasicSalary())
                .build();

        List<EmployeeBenefit> benefits = request.getBenefits()
                .stream()
                .map( b -> benefitMapper.toEntity(
                        EmployeeBenefitRequest.builder()
                                .benefitCode(b.getBenefitCode())
                                .amount(b.getAmount())
                                .build()
                )).
                toList();

        benefits.forEach(b -> b.setEmployee(employee));
        employee.setBenefits(benefits);

        employee.setSalary(salary);
        employee.setGovernmentId(governmentId);

        return employee;
    }

    public void updateEntity(Employee existing, EmployeeRequest request) {

        // --- BASIC INFO ---
        existing.setFirstName(request.getFirstName());
        existing.setLastName(request.getLastName());
        existing.setBirthday(request.getBirthday());
        existing.setAddress(request.getAddress());
        existing.setPhoneNumber(request.getPhoneNumber());

        // --- GOVERNMENT IDs ---
        if (request.getGovernmentId() != null) {
            if (existing.getGovernmentId() == null) {
                existing.setGovernmentId(new GovernmentId());
                existing.getGovernmentId().setEmployee(existing);
            }
            GovernmentId gov = existing.getGovernmentId();
            gov.setSssNumber(request.getGovernmentId().getSssNumber());
            gov.setTinNumber(request.getGovernmentId().getTinNumber());
            gov.setPhilhealthNumber(request.getGovernmentId().getPhilhealthNumber());
            gov.setPagIbigNumber(request.getGovernmentId().getPagIbigNumber());
        }

        existing.setStatus(request.getStatus());
        existing.setStartShift(request.getStartShift());
        existing.setEndShift(request.getEndShift());

        existing.getSalary().setBaseAmount(request.getBasicSalary());

        List<EmployeeBenefit> benefits = request.getBenefits()
                .stream()
                .map( b -> benefitMapper.toEntity(
                        EmployeeBenefitRequest.builder()
                                .benefitCode(b.getBenefitCode())
                                .amount(b.getAmount())
                                .build()
                )).
                toList();

        benefits.forEach(b -> b.setEmployee(existing));
        existing.getBenefits().clear();
        existing.getBenefits().addAll(benefits);
    }

}
