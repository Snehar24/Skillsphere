package com.skillsphere.dashboard.dto;

import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.student.dto.StudentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDashboardResponse {
    private StudentResponse profile;
    private List<BatchResponse> enrolledBatches;
    private List<com.skillsphere.batch.dto.EnrollmentResponse> enrollments;

    private double attendancePercentage;
    private long assignmentCount;
    private long examCount;
    private long projectCount;
    private long certificateCount;
    private long paymentCount;
    private long notificationCount;
}
