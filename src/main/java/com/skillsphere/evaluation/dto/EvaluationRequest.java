package com.skillsphere.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EvaluationRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    private Double technicalScore;
    private Double communicationScore;
    private Double attendanceScore;
    private Double assignmentScore;
    private Double projectScore;
    private String feedback;
}
