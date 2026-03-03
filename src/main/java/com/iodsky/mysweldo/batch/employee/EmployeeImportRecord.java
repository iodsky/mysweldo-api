package com.iodsky.mysweldo.batch.employee;


import com.iodsky.mysweldo.batch.DateTimeUtil;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.GovernmentId;
import com.iodsky.mysweldo.employee.Status;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeImportRecord {

    public static final String[] CSV_COLUMN_NAMES = {
            "lastName",
            "firstName",
            "birthday",
            "address",
            "phoneNumber",
            "sssNumber",
            "philhealthNumber",
            "tinNumber",
            "pagIbigNumber",
            "status",
            "position",
            "supervisorId",
            "startShift",
            "endShift",
            "basicSalary",
            "mealAllowance",
            "phoneAllowance",
            "clothingAllowance"
    };

    private String lastName;
    private String firstName;
    private String birthday;
    private String address;
    private String phoneNumber;
    private String sssNumber;
    private String philhealthNumber;
    private String tinNumber;
    private String pagIbigNumber;
    private String status;
    private String position;
    private String supervisorId;
    private String startShift;
    private String endShift;
    private String basicSalary;
    private String mealAllowance;
    private String phoneAllowance;
    private String clothingAllowance;

    public static Employee toEntity(EmployeeImportRecord record) {

        GovernmentId governmentId = GovernmentId.builder()
                .sssNumber(record.getSssNumber())
                .philhealthNumber(record.getPhilhealthNumber())
                .tinNumber(record.getTinNumber())
                .pagIbigNumber(record.getPagIbigNumber())
                .build();

        BigDecimal basicSalary = new BigDecimal(record.getBasicSalary());
        BigDecimal semiMonthlyRate = basicSalary.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal hourlyRate = basicSalary.divide(BigDecimal.valueOf(21.75).multiply(BigDecimal.valueOf(8)), 2, RoundingMode.HALF_UP);

        Employee employee = Employee.builder()
                .lastName(record.getLastName())
                .firstName(record.getFirstName())
                .birthday(DateTimeUtil.parseDate(record.getBirthday()))
                .address(record.getAddress())
                .phoneNumber(record.getPhoneNumber())
                .governmentId(governmentId)
                .status(Status.valueOf(record.getStatus().toUpperCase()))
                .startShift(DateTimeUtil.parseTime(record.getStartShift()))
                .endShift(DateTimeUtil.parseTime(record.getEndShift()))
                .basicSalary(basicSalary)
                .semiMonthlyRate(semiMonthlyRate)
                .hourlyRate(hourlyRate)
                .build();

        governmentId.setEmployee(employee);

        return employee;
    }

}
