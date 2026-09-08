package com.skillsphere.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CertificateResponse {
    private Long id;
    private String certificateNumber;
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private Long courseId;
    private String courseName;
    private LocalDate issueDate;
    private Double score;
    private String status;
    private String verificationCode;
    private String grade;
    private Long trainerId;
    private String trainerName;
}
