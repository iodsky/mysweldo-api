package com.iodsky.mysweldo.payroll.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.contribution.Contribution;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "employer_contribution",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"payroll_item_id", "contribution_code"}
        )
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmployerContribution extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payroll_item_id", nullable = false)
    @JsonIgnore
    private PayrollItem payrollItem;

    @ManyToOne
    @JoinColumn(name = "contribution_code", nullable = false)
    private Contribution contribution;

    @Column(nullable = false)
    private BigDecimal amount;

}
