package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_run")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollRun extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private PayrollPeriod period;

    @Enumerated(EnumType.STRING)
    private PayrollRunType type;

    @Enumerated(EnumType.STRING)
    private PayrollRunStatus status;

    @Column(name = "total_gross_pay")
    private BigDecimal totalGrossPay;

    @Column(name = "total_net_pay")
    private BigDecimal totalNetPay;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "total_benefits")
    private BigDecimal totalBenefits;

    @Column(name = "total_employer_cost")
    private BigDecimal totalEmployerCost;

    private String notes;

}
