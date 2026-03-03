package com.iodsky.sweldox.leave.credit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.sweldox.common.BaseModel;
import com.iodsky.sweldox.employee.Employee;
import com.iodsky.sweldox.leave.LeaveType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "leave_credit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LeaveCredit extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @Enumerated(value = EnumType.STRING)
    private LeaveType type;

    @Min(value = 0, message = "Leave credits cannot be negative")
    private double credits;

    private LocalDate effectiveDate;

}
