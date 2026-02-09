package com.iodsky.sweldox.leave.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.sweldox.common.BaseModel;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.leave.LeaveType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "leave_request",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "startDate", "endDate"}
        )
)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaveRequest extends BaseModel {

    @Id
    @LeaveRequestId
    private String id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @Enumerated(EnumType.STRING)
    private LeaveType leaveType;

    private LocalDate startDate;

    private LocalDate endDate;

    private String note;

    @Enumerated(value = EnumType.STRING)
    private LeaveStatus leaveStatus;


}
