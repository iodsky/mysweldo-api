package com.iodsky.mysweldo.security.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UserRequest {

    @NotNull
    @Min(1001)
    private Long employeeId;

    @NotNull
    @NotEmpty
    private String role;

    @NotNull
    @Email
    private String email;

    @NotNull
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

}
