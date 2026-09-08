package com.skillsphere.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssignmentSubmissionRequest {
    @NotBlank(message = "Answer cannot be blank")
    private String answer;
}
