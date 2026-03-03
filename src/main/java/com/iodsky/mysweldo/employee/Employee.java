package com.iodsky.mysweldo.employee;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.department.Department;
import com.iodsky.mysweldo.position.Position;
import com.iodsky.mysweldo.benefit.Benefit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "employee")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Employee extends BaseModel {

    @SequenceGenerator(
            name = "employee_id_seq",
            sequenceName = "employee_id_seq",
            initialValue = 10001,
            allocationSize = 1
    )
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "employee_id_seq")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    private LocalDate birthday;

    @Column(unique = true)
    private String address;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, optional = true)
    @JsonIgnore
    private GovernmentId governmentId;

    @ManyToOne
    @JoinColumn(name = "supervisor_id", nullable = true)
    @JsonIgnore
    private Employee supervisor;

    @ManyToOne
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalTime startShift;

    private LocalTime endShift;

    @Column(name = "basic_salary")
    private BigDecimal basicSalary;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Column(name = "semi_monthly_rate")
    private BigDecimal semiMonthlyRate;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnore
    private List<Benefit> benefits;

}
