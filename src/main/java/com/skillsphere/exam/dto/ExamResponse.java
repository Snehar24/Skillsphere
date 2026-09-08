package com.skillsphere.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {
    private Long id;
    private String title;
    private String description;
    private Long courseId;
    private String courseName;
    private Long batchId;
    private String batchName;
    private Integer duration;
    private Integer totalMarks;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private int questionCount;
    private Integer allowedAttempts;
    private long attemptsUsed;
    private long attemptsRemaining;
}
