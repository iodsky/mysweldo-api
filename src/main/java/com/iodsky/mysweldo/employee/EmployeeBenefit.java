package com.iodsky.mysweldo.employee;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.benefit.Benefit;
import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "employee_benefit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployeeBenefit extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "benefit_code")
    @JsonIgnore
    private Benefit benefit;

    private BigDecimal amount;

    public BigDecimal getTaxableAmount() {
        if (amount == null || benefit == null) {
            return BigDecimal.ZERO;
        }

        if (benefit.isTaxable()) {
            return amount;
        }

        BigDecimal limit = benefit.getNonTaxableLimit();
        if (limit == null) {
            return BigDecimal.ZERO;
        }

        return amount.subtract(limit).max(BigDecimal.ZERO);
    }

    public BigDecimal getNonTaxableAmount() {
        if (amount == null || benefit == null) {
            return BigDecimal.ZERO;
        }

        if (benefit.isTaxable()) {
            return BigDecimal.ZERO;
        }

        BigDecimal limit = benefit.getNonTaxableLimit();
        if (limit == null) {
            return amount;
        }

        return amount.min(limit);
    }

}
