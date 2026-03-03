package com.iodsky.mysweldo.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PositionUpdateRequest {

    @NotBlank(message = "Department ID is required")
    @Size(max = 20, message = "Department ID must not exceed 20 characters")
    private String departmentId;

    @NotBlank(message = "Position title is required")
    @Size(max = 255, message = "Position title must not exceed 255 characters")
    private String title;
}
