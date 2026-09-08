package com.skillsphere.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectEvaluationRequest {
    @NotNull(message = "Marks are required")
    private Integer marks;

    private String feedback;
}
