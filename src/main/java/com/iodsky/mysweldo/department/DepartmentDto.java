package com.iodsky.mysweldo.department;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DepartmentDto {

    private String id;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}
