package com.iodsky.mysweldo.pagIbig;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "pagibig_rate")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagibigRate extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal employeeRate;

    @Column(name = "employer_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal employerRate;

    @Column(name = "low_income_threshold")
    private BigDecimal lowIncomeThreshold;

    @Column(name = "low_income_employee_rate", precision = 5, scale = 4)
    private BigDecimal lowIncomeEmployeeRate;

    @Column(name = "max_salary_cap", nullable = false)
    private BigDecimal maxSalaryCap;

    @Column(name = "effective_date", nullable = false, unique = true)
    private LocalDate effectiveDate;

}
