package com.skillsphere.exam.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ExamSubmitRequest {
    // Map of questionId -> student selected option (A, B, C, or D)
    private Map<Long, String> answers;
    private String submissionType; // MANUAL, TIMEOUT, FULLSCREEN_VIOLATION
    private Integer violationCount;
}
