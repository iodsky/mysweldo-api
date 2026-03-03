package com.iodsky.mysweldo.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Department ID is required")
    @Size(max = 20, message = "Department ID must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Department ID must contain only uppercase letters, numbers, and underscores")
    private String id;

    @NotBlank(message = "Department title is required")
    @Size(max = 255, message = "Department title must not exceed 255 characters")
    private String title;
}
