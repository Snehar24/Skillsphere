package com.skillsphere.batch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BatchRequest {
    @NotBlank(message = "Batch name is required")
    private String batchName;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    private Long trainerId; // Optional initial assignment

    private LocalDate startDate;
    private LocalDate endDate;

    @NotNull(message = "Max students count is required")
    private Integer maxStudents;

    private String status;

    private String meetingLink;
    private String meetingTitle;
}
