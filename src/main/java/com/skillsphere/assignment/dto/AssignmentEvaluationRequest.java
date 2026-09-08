package com.skillsphere.assignment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignmentEvaluationRequest {
    @NotNull(message = "Marks are required")
    private Integer marks;

    private String feedback;
}
