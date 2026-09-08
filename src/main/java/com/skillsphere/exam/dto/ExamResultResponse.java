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
public class ExamResultResponse {
    private Long id;
    private Long examId;
    private String examTitle;
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private String batchName;
    private Integer attemptNumber;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer wrongAnswers;
    private Integer maxMarks;
    private Integer score;
    private Double percentage;
    private String result;
    private String submissionType;
    private Integer violationCount;
    private LocalDateTime submittedAt;
}
