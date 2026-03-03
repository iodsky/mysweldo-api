package com.iodsky.mysweldo.department;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "department")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Department extends BaseModel {

    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false, unique = true)
    private String title;
}
