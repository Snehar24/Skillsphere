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
public class ExamAttemptResponse {
    private Long attemptId;
    private Integer attemptNumber;
    private LocalDateTime startTime;
    private Integer duration;
    private Integer allowedAttempts;
    private Integer attemptsUsed;
}
