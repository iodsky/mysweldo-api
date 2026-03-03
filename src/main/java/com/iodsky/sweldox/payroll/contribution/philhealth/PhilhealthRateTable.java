package com.iodsky.sweldox.payroll.contribution.philhealth;

import com.iodsky.sweldox.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "philhealth_rate_table")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PhilhealthRateTable extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "premium_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal premiumRate;

    @Column(name = "max_salary_cap", nullable = false)
    private BigDecimal maxSalaryCap;

    @Column(name = "min_salary_floor", nullable = false)
    private BigDecimal minSalaryFloor;

    @Column(name = "fixed_contribution", nullable = false)
    private BigDecimal fixedContribution;

    @Column(name = "effective_date", nullable = false, unique = true)
    private LocalDate effectiveDate;

}

