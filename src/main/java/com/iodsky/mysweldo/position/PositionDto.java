package com.iodsky.mysweldo.position;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PositionDto {

    private String id;
    private String departmentId;
    private String departmentTitle;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}
