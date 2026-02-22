package com.iodsky.sweldox.overtime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.sweldox.common.BaseModel;
import com.iodsky.sweldox.common.RequestStatus;
import com.iodsky.sweldox.employee.Employee;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name="overtime_request", uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "date"}))
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OvertimeRequest extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    private LocalDate date;

    @Column(name = "overtime_hours")
    private BigDecimal overtimeHours;

    private String reason;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

}
