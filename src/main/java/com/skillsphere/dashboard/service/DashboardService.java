package com.skillsphere.dashboard.service;

import com.skillsphere.assignment.repository.AssignmentRepository;
import com.skillsphere.attendance.repository.AttendanceRepository;
import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.entity.Enrollment;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.certificate.repository.CertificateRepository;
import com.skillsphere.common.enums.BatchStatus;
import com.skillsphere.common.enums.Role;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.dashboard.dto.AdminDashboardResponse;
import com.skillsphere.dashboard.dto.StudentDashboardResponse;
import com.skillsphere.dashboard.dto.TrainerDashboardResponse;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.exam.repository.ExamRepository;
import com.skillsphere.exam.repository.ExamResultRepository;
import com.skillsphere.notification.repository.NotificationRepository;
import com.skillsphere.payment.dto.PaymentResponse;
import com.skillsphere.payment.entity.Payment;
import com.skillsphere.payment.repository.PaymentRepository;
import com.skillsphere.project.repository.ProjectRepository;
import com.skillsphere.student.dto.StudentResponse;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TrainerRepository trainerRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExamRepository examRepository;
    private final ProjectRepository projectRepository;
    private final CertificateRepository certificateRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;
    private final ExamResultRepository examResultRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final com.skillsphere.batch.service.BatchService batchService;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        long totalStudents = userRepository.countByRole(Role.ROLE_STUDENT);
        long totalTrainers = userRepository.countByRole(Role.ROLE_TRAINER);
        long totalCourses = courseRepository.count();
        long totalBatches = batchRepository.count();
        long activeBatches = batchRepository.countByStatus(BatchStatus.ACTIVE);
        long totalAssignments = assignmentRepository.count();
        long totalExams = examRepository.count();
        long totalCertificates = certificateRepository.count();

        BigDecimal revenue = paymentRepository.sumPaidPayments();
        BigDecimal totalRevenue = revenue != null ? revenue : BigDecimal.ZERO;

        // Fetch recent lists
        List<StudentResponse> recentStudents = studentRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent().stream().map(this::mapToStudentResponse).collect(Collectors.toList());

        List<BatchResponse> recentBatches = batchRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent().stream().map(this::mapToBatchResponse).collect(Collectors.toList());

        List<PaymentResponse> recentPayments = paymentRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent().stream().map(this::mapToPaymentResponse).collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalStudents(totalStudents)
                .totalTrainers(totalTrainers)
                .totalCourses(totalCourses)
                .totalBatches(totalBatches)
                .activeBatches(activeBatches)
                .totalAssignments(totalAssignments)
                .totalExams(totalExams)
                .totalRevenue(totalRevenue)
                .totalCertificates(totalCertificates)
                .recentStudents(recentStudents)
                .recentBatches(recentBatches)
                .recentPayments(recentPayments)
                .build();
    }

    @Transactional(readOnly = true)
    public TrainerDashboardResponse getTrainerDashboard(Long trainerUserId) {
        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer profile not found"));

        List<Batch> assignedBatches = batchRepository.findByTrainer(trainer);

        long studentCount = assignedBatches.stream()
                .mapToLong(b -> enrollmentRepository.countActiveEnrollmentsByBatchId(b.getId()))
                .sum();

        long courseCount = assignedBatches.stream()
                .map(Batch::getCourse)
                .distinct()
                .count();

        long assignmentCount = assignmentRepository.findByTrainerId(trainer.getId()).size();
        long projectCount = projectRepository.findByTrainerId(trainer.getId()).size();
        // Exams are associated with batches, sum exams in assigned batches
        long examCount = assignedBatches.stream()
                .mapToLong(b -> examRepository.findByBatchId(b.getId()).size())
                .sum();

        List<BatchResponse> batchResponses = assignedBatches.stream()
                .map(this::mapToBatchResponse)
                .collect(Collectors.toList());

        return TrainerDashboardResponse.builder()
                .batchCount(assignedBatches.size())
                .studentCount(studentCount)
                .courseCount(courseCount)
                .assignmentCount(assignmentCount)
                .examCount(examCount)
                .projectCount(projectCount)
                .assignedBatches(batchResponses)
                .build();
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse getStudentDashboard(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student profile not found"));

        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<BatchResponse> enrolledBatches = enrollments.stream()
                .filter(e -> e.getBatch() != null)
                .map(e -> mapToBatchResponse(e.getBatch()))
                .collect(Collectors.toList());

        List<com.skillsphere.batch.dto.EnrollmentResponse> mappedEnrollments = enrollments.stream()
                .map(batchService::mapToEnrollmentResponse)
                .collect(Collectors.toList());

        // Calculate cumulative attendance across all enrolled batches consistently
        Map<String, Object> overallAttSummary = attendanceService.getStudentAttendanceSummary(student.getId(), null);
        Object percentageObj = overallAttSummary.get("percentage");
        double avgAttendance = 0.0;
        if (percentageObj instanceof Number) {
            avgAttendance = ((Number) percentageObj).doubleValue();
        }

        long assignmentCount = 0;
        long examCount = 0;
        long projectCount = 0;

        for (Enrollment enrollment : enrollments) {
            if (enrollment.getBatch() == null) continue;
            assignmentCount += assignmentRepository.findByBatchId(enrollment.getBatch().getId()).size();
            examCount += examRepository.findByBatchId(enrollment.getBatch().getId()).size();
            projectCount += projectRepository.findByBatchId(enrollment.getBatch().getId()).size();
        }

        long certificateCount = certificateRepository.findByStudentId(student.getId()).size();
        long paymentCount = paymentRepository.findByStudentId(student.getId()).size();
        long notificationCount = notificationRepository.countByUserIdAndReadFalse(studentUserId);

        return StudentDashboardResponse.builder()
                .profile(mapToStudentResponse(student))
                .enrolledBatches(enrolledBatches)
                .enrollments(mappedEnrollments)
                .attendancePercentage(Math.round(avgAttendance * 100.0) / 100.0)
                .assignmentCount(assignmentCount)
                .examCount(examCount)
                .projectCount(projectCount)
                .certificateCount(certificateCount)
                .paymentCount(paymentCount)
                .notificationCount(notificationCount)
                .build();
    }

    private StudentResponse mapToStudentResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .phone(student.getUser().getPhone())
                .registrationNumber(student.getRegistrationNumber())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .qualification(student.getQualification())
                .address(student.getAddress())
                .joiningDate(student.getJoiningDate())
                .status(student.getStatus())
                .active(student.getUser().isActive())
                .build();
    }

    private BatchResponse mapToBatchResponse(Batch batch) {
        long activeCount = enrollmentRepository.countActiveEnrollmentsByBatchId(batch.getId());
        return BatchResponse.builder()
                .id(batch.getId())
                .batchName(batch.getBatchName())
                .courseId(batch.getCourse().getId())
                .courseName(batch.getCourse().getName())
                .trainerId(batch.getTrainer() != null ? batch.getTrainer().getId() : null)
                .trainerName(batch.getTrainer() != null ? batch.getTrainer().getUser().getName() : "UNASSIGNED")
                .startDate(batch.getStartDate())
                .endDate(batch.getEndDate())
                .maxStudents(batch.getMaxStudents())
                .status(batch.getStatus().name())
                .activeStudentsCount(activeCount)
                .meetingLink(batch.getMeetingLink())
                .meetingTitle(batch.getMeetingTitle())
                .build();
    }

    private PaymentResponse mapToPaymentResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .studentId(p.getStudent().getId())
                .studentName(p.getStudent().getUser().getName())
                .registrationNumber(p.getStudent().getRegistrationNumber())
                .courseId(p.getCourse().getId())
                .courseName(p.getCourse().getName())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .status(p.getStatus().name())
                .build();
    }
}
