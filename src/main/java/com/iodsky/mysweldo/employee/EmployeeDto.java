package com.iodsky.mysweldo.employee;

import com.iodsky.mysweldo.batch.employee.EmployeeBenefitDto;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class EmployeeDto {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthday;
    private String address;
    private String phoneNumber;
    private String sssNumber;
    private String tinNumber;
    private String philhealthNumber;
    private String pagIbigNumber;
    private String supervisor;
    private String position;
    private String department;
    private String status;
    private LocalTime startShift;
    private LocalTime endShift;
    private BigDecimal basicSalary;
    private BigDecimal hourlyRate;
    private BigDecimal semiMonthlyRate;
    private List<EmployeeBenefitDto> benefits;

}
