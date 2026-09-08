package com.skillsphere.batch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    private Long id;
    private String batchName;
    private Long courseId;
    private String courseName;
    private Long trainerId;
    private String trainerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private int maxStudents;
    private String status;
    private long activeStudentsCount;
    private String meetingLink;
    private String meetingTitle;
}
