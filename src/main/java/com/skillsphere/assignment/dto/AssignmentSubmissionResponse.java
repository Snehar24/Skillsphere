package com.skillsphere.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionResponse {
    private Long id;
    private Long assignmentId;
    private String assignmentTitle;
    private Long studentId;
    private String studentName;
    private String answer;
    private Integer marks;
    private String feedback;
    private String status;
    private LocalDateTime submissionDate;
    private String submissionDocument;
    private String matchedKeywords;
    private String missingKeywords;
    private Double percentage;
    private LocalDateTime evaluationDate;
}
