package com.iodsky.mysweldo.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentUpdateRequest {

    @NotBlank(message = "Department title is required")
    @Size(max = 255, message = "Department title must not exceed 255 characters")
    private String title;
}
