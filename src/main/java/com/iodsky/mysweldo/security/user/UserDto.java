package com.iodsky.mysweldo.security.user;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private Long employeeId;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String role;
   private Instant createdAt;
   private Instant updatedAt;

}
