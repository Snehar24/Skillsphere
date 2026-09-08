package com.skillsphere.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourseRequest {
    @NotBlank(message = "Course name is required")
    private String name;

    private String description;
    private String duration;

    @NotNull(message = "Fee is required")
    private BigDecimal fee;

    private String status;
}
