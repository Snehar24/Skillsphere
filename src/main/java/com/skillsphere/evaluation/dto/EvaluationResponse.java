package com.skillsphere.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private Long trainerId;
    private String trainerName;
    private Long batchId;
    private String batchName;
    private Double technicalScore;
    private Double communicationScore;
    private Double attendanceScore;
    private Double assignmentScore;
    private Double projectScore;
    private Double overallScore;
    private String feedback;
    private LocalDateTime evaluatedAt;
}
