package com.skillsphere.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationEligibilityResponse {
    private Long studentId;
    private String studentName;
    private String email;
    private String registrationNumber;

    private Long batchId;
    private String batchName;
    private Long courseId;
    private String courseName;

    private double attendancePercentage;
    private long totalClasses;
    private long attendedClasses;

    private double examAverageScore;
    private boolean examPassed;
    private int examCount;

    private double assignmentScore;
    private double projectScore;
    private double overallScore;
    private String suggestedGrade;

    private boolean eligibleForCertificate;
    private String eligibilityReason;

    private boolean certificateIssued;
    private String certificateNumber;
    private String verificationCode;
    private String grade;
}
