package com.skillsphere.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSubmissionResponse {
    private Long id;
    private Long projectId;
    private String projectTitle;
    private Long studentId;
    private String studentName;
    private String repositoryUrl;
    private String documentation;
    private Integer marks;
    private String feedback;
    private String status;
    private LocalDateTime submittedAt;
    private String submissionDocument;
    private String matchedKeywords;
    private String missingKeywords;
    private Double percentage;
    private LocalDateTime evaluationDate;
}
