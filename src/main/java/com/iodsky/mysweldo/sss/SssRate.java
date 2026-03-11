package com.iodsky.mysweldo.sss;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "sss_rate")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SssRate extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "total_sss", nullable = false)
    private BigDecimal totalSss;

    @Column(name = "employee_sss", nullable = false, precision = 5, scale = 4)
    private BigDecimal employeeRate;

    @Column(name = "employer_sss", nullable = false, precision = 5, scale = 4)
    private BigDecimal employerRate;

    @JdbcTypeCode(SqlTypes.JSON) // Maps java list to sql json type
    @Column(name = "salary_brackets", nullable = false, columnDefinition = "jsonb") // PostgreSQL binary JSON - more efficient than regular JSON
    private List<SalaryBracket> salaryBrackets;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalaryBracket {
        private BigDecimal minSalary;
        private BigDecimal maxSalary;
        private BigDecimal msc;
    }

    public SalaryBracket findBracket(BigDecimal salary) {
        return salaryBrackets.stream()
                .filter(bracket -> {
                    if (salary.compareTo(bracket.getMinSalary()) < 0) {
                        return false;
                    }

                    if (bracket.getMaxSalary() == null) {
                        return true;
                    }

                    return salary.compareTo(bracket.getMaxSalary()) <= 0;
                })
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No SSS bracket found for salary: " + salary
                ));
    }


}

