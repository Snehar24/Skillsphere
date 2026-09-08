package com.skillsphere.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentPerformanceReportResponse {
    private Long studentId;
    private Long userId;
    private String studentName;
    private String email;
    private String phone;
    private String registrationNumber;

    private Long courseId;
    private String courseName;
    private Long batchId;
    private String batchName;

    private double attendancePercentage;
    private long totalClasses;
    private long presentClasses;

    private int examCount;
    private double averageExamScore;
    private String examStatus;

    private int assignmentsSubmitted;
    private double averageAssignmentScore;

    private int projectsSubmitted;
    private String projectStatus;

    private double overallScore;
    private String grade;

    private String paymentStatus;
    private BigDecimal amountPaid;
    private BigDecimal totalCourseFee;

    private boolean certificateIssued;
    private String certificateNumber;
    private String verificationCode;
    private LocalDate certificateIssueDate;
}
