package com.iodsky.mysweldo.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PositionRequest {

    @NotBlank(message = "Position ID is required")
    @Size(max = 20, message = "Position ID must not exceed 20 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Position ID must contain only uppercase letters, numbers, and underscores")
    private String id;

    @NotBlank(message = "Department ID is required")
    @Size(max = 20, message = "Department ID must not exceed 20 characters")
    private String departmentId;

    @NotBlank(message = "Position title is required")
    @Size(max = 255, message = "Position title must not exceed 255 characters")
    private String title;
}
