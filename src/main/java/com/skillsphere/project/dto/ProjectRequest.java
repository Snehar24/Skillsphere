package com.skillsphere.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProjectRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    private LocalDate deadline;
    private Integer totalMarks;
    private String status;
    private String trainerContent;
    private String requiredKeywords;
}
