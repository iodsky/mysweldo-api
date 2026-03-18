package com.iodsky.mysweldo.payroll.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.employee.Employee;
import com.iodsky.mysweldo.employee.SalaryType;
import com.iodsky.mysweldo.payroll.run.PayrollRun;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "payroll_item",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"payroll_run_id", "employee_id"}
        )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollItem extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @Column(name = "days_worked")
    private BigDecimal daysWorked;

    @Column(name = "monthly_rate")
    private BigDecimal monthlyRate;

    @Column(name = "semi_monthly_rate")
    private BigDecimal semiMonthlyRate;

    @Column(name = "daily_rate")
    private BigDecimal dailyRate;

    @Column(name = "hourly_rate")
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type")
    private SalaryType salaryType;

    private BigDecimal absences;

    @Column(name = "tardiness_minutes")
    private Integer tardinessMinutes;

    @Column(name = "undertime_minutes")
    private Integer undertimeMinutes;

    @Column(name = "overtime_minutes")
    private Integer overtimeMinutes;

    @Column(name = "ovetime_pay")
    private BigDecimal overtimePay;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PayrollBenefit> benefits;

    @Column(name = "total_benefits")
    private BigDecimal totalBenefits;

    @Column(name = "gross_pay")
    private BigDecimal grossPay;

    @OneToMany(mappedBy = "payroll", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<PayrollDeduction> deductions;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "net_pay")
    private BigDecimal netPay;

}
