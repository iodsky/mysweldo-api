package com.iodsky.mysweldo.employee;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "salary")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Salary extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "employee_id", unique = true)
    @JsonIgnore
    private Employee employee;

    @Column(name = "base_amount")
    private BigDecimal baseAmount;

    @Enumerated(EnumType.STRING)
    private SalaryType type;

    @Column(name = "effective_date")
    @Builder.Default
    private LocalDate effectiveDate = LocalDate.now();

}
