package com.skillsphere.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private String courseName;
    private Long batchId;
    private String batchName;
    private Long trainerId;
    private String trainerName;
    private LocalDate dueDate;
    private Integer totalMarks;
    private String status;
    private String trainerContent;
    private String requiredKeywords;
}
