package com.iodsky.mysweldo.employee;


import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.benefit.BenefitDto;
import com.iodsky.mysweldo.benefit.BenefitMapper;
import com.iodsky.mysweldo.benefit.BenefitRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
@RequiredArgsConstructor
public class EmployeeMapper  {

    private final BenefitMapper benefitMapper;

    public EmployeeDto toDto(Employee employee) {

        Employee supervisor = employee.getSupervisor();
        String supervisorName = supervisor != null ? supervisor.getFirstName() + " " + supervisor.getLastName() : "N/A";

        List<BenefitDto> benefits = employee.getBenefits()
                .stream()
                .map(b -> BenefitDto.builder()
                        .benefit(b.getBenefitType().getId().toLowerCase())
                        .amount(b.getAmount())
                        .build())
                .toList();

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
                .supervisor(supervisorName)
                .department(employee.getDepartment().getTitle())
                .position(employee.getPosition().getTitle())
                .startShift(employee.getStartShift())
                .endShift(employee.getEndShift())
                .basicSalary(employee.getBasicSalary())
                .hourlyRate(employee.getHourlyRate())
                .semiMonthlyRate(employee.getSemiMonthlyRate())
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

        BigDecimal basicSalary = request.getBasicSalary();
        BigDecimal semiMonthlyRate = basicSalary.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = basicSalary.divide(BigDecimal.valueOf(21.75).multiply(BigDecimal.valueOf(8)), 2, RoundingMode.HALF_UP);

        employee.setBasicSalary(basicSalary);
        employee.setSemiMonthlyRate(semiMonthlyRate);
        employee.setHourlyRate(hourlyRate);

        List<Benefit> benefits = request.getBenefits()
                .stream()
                .map( b -> benefitMapper.toEntity(
                        BenefitRequest.builder()
                                .benefitTypeId(b.getBenefitTypeId())
                                .amount(b.getAmount())
                                .build()
                )).
                toList();

        benefits.forEach(b -> b.setEmployee(employee));
        employee.setBenefits(benefits);

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

        BigDecimal basicSalary = request.getBasicSalary();
        BigDecimal semiMonthlyRate = basicSalary.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = basicSalary.divide(BigDecimal.valueOf(21.75).multiply(BigDecimal.valueOf(8)), 2, RoundingMode.HALF_UP);

        existing.setBasicSalary(basicSalary);
        existing.setSemiMonthlyRate(semiMonthlyRate);
        existing.setHourlyRate(hourlyRate);

        List<Benefit> benefits = request.getBenefits()
                .stream()
                .map( b -> benefitMapper.toEntity(
                        BenefitRequest.builder()
                                .benefitTypeId(b.getBenefitTypeId())
                                .amount(b.getAmount())
                                .build()
                )).
                toList();

        benefits.forEach(b -> b.setEmployee(existing));
        existing.getBenefits().clear();
        existing.getBenefits().addAll(benefits);
    }

}
