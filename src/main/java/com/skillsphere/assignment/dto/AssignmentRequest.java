package com.skillsphere.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    private LocalDate dueDate;
    private Integer totalMarks;
    private String status;
    private String trainerContent;
    private String requiredKeywords;
}
