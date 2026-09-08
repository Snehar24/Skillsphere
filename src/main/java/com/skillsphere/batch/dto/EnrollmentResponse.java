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
public class EnrollmentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private Long batchId;
    private String batchName;
    private Long courseId;
    private String courseName;
    private LocalDate enrollmentDate;
    private String status;
    private Long pendingPaymentId;
    private String trainerName;
    private LocalDate startDate;
    private LocalDate endDate;
    private String meetingLink;
    private String meetingTitle;
    private String paymentStatus;
}
