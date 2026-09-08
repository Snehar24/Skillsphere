package com.skillsphere.dashboard.dto;

import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.payment.dto.PaymentResponse;
import com.skillsphere.student.dto.StudentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalStudents;
    private long totalTrainers;
    private long totalCourses;
    private long totalBatches;
    private long activeBatches;
    private long totalAssignments;
    private long totalExams;
    private BigDecimal totalRevenue;
    private long totalCertificates;

    private List<StudentResponse> recentStudents;
    private List<BatchResponse> recentBatches;
    private List<PaymentResponse> recentPayments;
}
