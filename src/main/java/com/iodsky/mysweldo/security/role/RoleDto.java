package com.iodsky.mysweldo.security.role;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;
@Data
@Builder
public class RoleDto {
    private Long id;
    private String name;
    private String description;
    private Instant lastModified;
    private UUID lastModifiedBy;
}

