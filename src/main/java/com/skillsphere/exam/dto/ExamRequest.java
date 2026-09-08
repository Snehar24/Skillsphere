package com.skillsphere.exam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExamRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Duration in minutes is required")
    private Integer duration;

    @NotNull(message = "Total marks is required")
    private Integer totalMarks;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Integer allowedAttempts = 1;
}
