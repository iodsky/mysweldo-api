package com.iodsky.mysweldo.security.user;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto {

   private UUID id;
   private String email;
   private Long employeeId;
   private String role;
   private Instant createdAt;
   private Instant updatedAt;

}
