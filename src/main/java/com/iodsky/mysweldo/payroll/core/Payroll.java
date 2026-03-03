package com.iodsky.mysweldo.payroll.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.payroll.deduction.Deduction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payroll",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"employee_id", "period_start_date", "period_end_date"}
        )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payroll extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Deduction> deductions;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PayrollBenefit> benefits;

    @Column(name = "period_start_date")
    private LocalDate periodStartDate;

    @Column(name = "period_end_date")
    private LocalDate periodEndDate;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "days_worked")
    private int daysWorked;

    private BigDecimal overtime;

    @Column(name = "monthly_rate")
    private BigDecimal monthlyRate;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    @Column(name = "gross_pay")
    private BigDecimal grossPay;

    @Column(name = "total_benefits")
    private BigDecimal totalBenefits;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "net_pay")
    private BigDecimal netPay;

}
