package com.iodsky.sweldox.benefit;

import com.iodsky.sweldox.common.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "benefit_type")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BenefitType extends BaseModel {

    @Id
    private String id;
    private String type;

}