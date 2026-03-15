package com.iodsky.mysweldo.deduction;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "deduction")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Deduction extends BaseModel {
    @Id
    private String code;
    private String description;
    @Builder.Default
    private boolean statutory = true;
}