package com.iodsky.mysweldo.payroll.item;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.iodsky.mysweldo.common.BaseModel;
import com.iodsky.mysweldo.deduction.Deduction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payroll_deduction")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayrollDeduction extends BaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "payroll_item_id")
    @JsonIgnore
    private PayrollItem payrollItem;

    @ManyToOne
    @JoinColumn(name = "deduction_code")
    private Deduction deduction;

    private BigDecimal amount;


}
