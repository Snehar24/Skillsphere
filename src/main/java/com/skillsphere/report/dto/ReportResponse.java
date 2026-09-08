package com.skillsphere.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private long totalStudents;
    private long totalTrainers;
    private long totalCourses;
    private long totalBatches;
    private long totalEnrollments;
    private long totalCertificates;
    private BigDecimal totalRevenue;

    private Map<String, Long> studentsPerCourse;
    private Map<String, Long> studentsPerBatch;
    private Map<String, Double> attendanceRatePerBatch;
}
