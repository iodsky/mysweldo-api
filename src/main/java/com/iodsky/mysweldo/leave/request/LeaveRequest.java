package com.iodsky.mysweldo.leave.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.common.RequestStatus;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.leave.LeaveType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

@Entity
@Table(
        name = "leave_request",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "startDate", "endDate"}
        )
)
@SQLRestriction("deleted_at IS NULL")
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
    private RequestStatus status;


}
