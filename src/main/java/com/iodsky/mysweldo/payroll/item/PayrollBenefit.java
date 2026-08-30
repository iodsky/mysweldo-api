package com.iodsky.mysweldo.payroll.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.benefit.Benefit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "payroll_benefit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollBenefit extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payroll_item_id")
    @JsonIgnore
    private PayrollItem payrollItem;

    @ManyToOne
    @JoinColumn(name = "benefit_code")
    private Benefit benefit;

    private BigDecimal amount;

}
