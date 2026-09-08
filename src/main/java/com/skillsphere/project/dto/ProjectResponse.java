package com.skillsphere.project.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private String title;
    private String description;
    private Long batchId;
    private String batchName;
    private Long trainerId;
    private String trainerName;
    private LocalDate deadline;
    private Integer totalMarks;
    private String status;
    private String trainerContent;
    private String requiredKeywords;
}
