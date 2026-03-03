package com.iodsky.sweldox.payroll.tax;

import com.iodsky.sweldox.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "income_tax_bracket",
        uniqueConstraints = @UniqueConstraint(columnNames = {"effective_date", "min_income"})
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class IncomeTaxBracket extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "min_income", nullable = false)
    private BigDecimal minIncome;

    @Column(name = "max_income")
    private BigDecimal maxIncome;

    @Column(name = "base_tax", nullable = false)
    private BigDecimal baseTax;

    @Column(name = "marginal_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal marginalRate;

    @Column(name = "threshold", nullable = false)
    private BigDecimal threshold;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

}
