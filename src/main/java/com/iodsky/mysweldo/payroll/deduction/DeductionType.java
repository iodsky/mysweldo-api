package com.iodsky.mysweldo.payroll.deduction;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "deduction_type")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeductionType extends BaseModel {
    @Id
    private String code;
    private String type;
}