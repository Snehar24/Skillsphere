package com.skillsphere.student.controller;

import com.skillsphere.assignment.dto.AssignmentResponse;
import com.skillsphere.assignment.dto.AssignmentSubmissionRequest;
import com.skillsphere.assignment.dto.AssignmentSubmissionResponse;
import com.skillsphere.assignment.service.AssignmentService;
import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.batch.service.BatchService;
import com.skillsphere.certificate.dto.CertificateResponse;
import com.skillsphere.certificate.service.CertificateService;
import com.skillsphere.dashboard.dto.StudentDashboardResponse;
import com.skillsphere.dashboard.service.DashboardService;
import com.skillsphere.exam.dto.ExamResponse;
import com.skillsphere.exam.dto.ExamResultResponse;
import com.skillsphere.exam.dto.ExamSubmitRequest;
import com.skillsphere.exam.dto.QuestionResponse;
import com.skillsphere.exam.service.ExamService;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.notification.dto.NotificationResponse;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.payment.dto.PaymentResponse;
import com.skillsphere.payment.service.PaymentService;
import com.skillsphere.project.dto.ProjectResponse;
import com.skillsphere.project.dto.ProjectSubmissionRequest;
import com.skillsphere.project.dto.ProjectSubmissionResponse;
import com.skillsphere.project.service.ProjectService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.attendance.dto.AttendanceResponse;
import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.evaluation.dto.EvaluationResponse;
import com.skillsphere.evaluation.service.EvaluationService;
import com.skillsphere.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final DashboardService dashboardService;
    private final BatchService batchService;
    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final ProjectService projectService;
    private final CertificateService certificateService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final AttendanceService attendanceService;
    private final EvaluationService evaluationService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final com.skillsphere.course.repository.CourseRepository courseRepository;
    private final com.skillsphere.batch.repository.EnrollmentRepository enrollmentRepository;
    private final com.skillsphere.batch.repository.BatchRepository batchRepository;
    private final com.skillsphere.course.service.CourseService courseService;
    private final com.skillsphere.course.service.CourseMaterialService courseMaterialService;
    private final com.skillsphere.batch.service.MeetingService meetingService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.skillsphere.common.service.AuditLogService auditLogService;

    private Long getStudentUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("Student user not found"));
        return user.getId();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<StudentDashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getStudentDashboard(getStudentUserId(userDetails)));
    }

    @GetMapping("/batches")
    public ResponseEntity<List<BatchResponse>> getEnrolledBatches(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        return ResponseEntity.ok(batchService.getStudentBatches(student.getId()));
    }

    // Assignments
    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentResponse>> getAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        List<BatchResponse> batches = batchService.getStudentBatches(student.getId());

        List<AssignmentResponse> assignments = new ArrayList<>();
        for (BatchResponse batch : batches) {
            assignments.addAll(assignmentService.getBatchAssignments(batch.getId()));
        }
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long assignmentId,
            @Valid @RequestBody AssignmentSubmissionRequest request) {
        return ResponseEntity.ok(assignmentService.submitAssignment(assignmentId, getStudentUserId(userDetails), request));
    }

    // Exams
    @GetMapping("/exams")
    public ResponseEntity<List<ExamResponse>> getExams(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(examService.getStudentExamsForDashboard(getStudentUserId(userDetails)));
    }

    @PostMapping("/exams/{examId}/start")
    public ResponseEntity<com.skillsphere.exam.dto.ExamAttemptResponse> startExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examService.startExamAttempt(examId, getStudentUserId(userDetails)));
    }

    @GetMapping("/exams/{examId}/questions")
    public ResponseEntity<List<QuestionResponse>> getExamQuestions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examService.getExamQuestionsForStudent(examId, getStudentUserId(userDetails)));
    }

    @PostMapping("/exams/{examId}/submit")
    public ResponseEntity<ExamResultResponse> submitExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId,
            @RequestBody ExamSubmitRequest request) {
        return ResponseEntity.ok(examService.submitExamAttempt(examId, getStudentUserId(userDetails), request));
    }

    @GetMapping("/exams/{examId}/result")
    public ResponseEntity<ExamResultResponse> getExamResult(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        return ResponseEntity.ok(examService.getExamResult(examId, getStudentUserId(userDetails)));
    }

    // Projects
    @GetMapping("/projects")
    public ResponseEntity<List<ProjectResponse>> getProjects(@AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        List<BatchResponse> batches = batchService.getStudentBatches(student.getId());

        List<ProjectResponse> projects = new ArrayList<>();
        for (BatchResponse batch : batches) {
            projects.addAll(projectService.getBatchProjects(batch.getId()));
        }
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/projects/{projectId}/submit")
    public ResponseEntity<ProjectSubmissionResponse> submitProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectSubmissionRequest request) {
        return ResponseEntity.ok(projectService.submitProject(projectId, getStudentUserId(userDetails), request));
    }

    // Certificates
    @GetMapping("/certificates")
    public ResponseEntity<List<CertificateResponse>> getCertificates(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(certificateService.getStudentCertificates(getStudentUserId(userDetails)));
    }

    // Payments
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getPayments(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(paymentService.getStudentPayments(getStudentUserId(userDetails)));
    }

    // Notifications
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(notificationService.getUserNotifications(getStudentUserId(userDetails)).stream()
                .map(n -> NotificationResponse.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .type(n.getType())
                        .read(n.isRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList());
    }

    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<Map<String, String>> markNotificationRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PostMapping("/notifications/{id}/unread")
    public ResponseEntity<Map<String, String>> markNotificationUnread(@PathVariable Long id) {
        notificationService.markAsUnread(id);
        return ResponseEntity.ok(Map.of("message", "Notification marked as unread"));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Map<String, String>> markAllNotificationsRead(@AuthenticationPrincipal UserDetails userDetails) {
        notificationService.markAllAsRead(getStudentUserId(userDetails));
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    // Attendance records
    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long batchId) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        return ResponseEntity.ok(attendanceService.getStudentAttendance(student.getId(), batchId));
    }

    // Attendance summary
    @GetMapping("/attendance/summary")
    public ResponseEntity<Map<String, Object>> getAttendanceSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long batchId) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        return ResponseEntity.ok(attendanceService.getStudentAttendanceSummary(student.getId(), batchId));
    }

    // Evaluations
    @GetMapping("/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getEvaluations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(evaluationService.getStudentEvaluations(getStudentUserId(userDetails)));
    }

    // Student password change
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.skillsphere.admin.dto.PasswordChangeRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log("PASSWORD_CHANGE", "User", user.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "Student changed password successfully", null);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // Exam Fullscreen / Tab switch violation
    @PostMapping("/exams/{examId}/violation")
    public ResponseEntity<Map<String, Object>> reportExamViolation(
            @PathVariable Long examId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(examService.recordViolation(examId, getStudentUserId(userDetails)));
    }

    // Enrollment
    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<com.skillsphere.batch.dto.EnrollmentResponse> enrollInCourse(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long courseId,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) Integer emiCount) {
        
        Long userId = getStudentUserId(userDetails);
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        
        if (!"ACTIVE".equalsIgnoreCase(student.getStatus())) {
            throw new BadRequestException("Student status is not active. Enrollment restricted.");
        }
        
        com.skillsphere.course.entity.Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new com.skillsphere.exception.ResourceNotFoundException("Course", courseId));
        
        com.skillsphere.batch.entity.Enrollment enrollment;
        java.util.Optional<com.skillsphere.batch.entity.Enrollment> existingOpt = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), courseId);
        
        if (existingOpt.isPresent()) {
            enrollment = existingOpt.get();
            if (enrollment.getStatus() == com.skillsphere.common.enums.EnrollmentStatus.ACTIVE) {
                throw new com.skillsphere.exception.DuplicateResourceException("You are already actively enrolled in this course.");
            }
            // If already pending, check if payment already exists
            List<PaymentResponse> existingPayments = paymentService.getStudentPayments(userId).stream()
                    .filter(p -> p.getCourseId().equals(courseId) && "PENDING".equalsIgnoreCase(p.getStatus()))
                    .collect(Collectors.toList());
            if (!existingPayments.isEmpty()) {
                com.skillsphere.batch.dto.EnrollmentResponse enrollRes = batchService.mapToEnrollmentResponse(enrollment);
                enrollRes.setPendingPaymentId(existingPayments.get(0).getId());
                return ResponseEntity.ok(enrollRes);
            }
        } else {
            enrollment = com.skillsphere.batch.entity.Enrollment.builder()
                    .student(student)
                    .course(course)
                    .batch(null)
                    .enrollmentDate(java.time.LocalDate.now())
                    .status(com.skillsphere.common.enums.EnrollmentStatus.PENDING)
                    .build();
            enrollment = enrollmentRepository.save(enrollment);
        }
        
        java.math.BigDecimal totalFee = course.getFee() != null ? course.getFee() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal paymentAmount;
        if ("EMI".equalsIgnoreCase(paymentMethod)) {
            int emis = (emiCount != null && emiCount > 0) ? emiCount : 3;
            paymentAmount = totalFee.divide(java.math.BigDecimal.valueOf(emis), 2, java.math.RoundingMode.HALF_UP);
        } else {
            paymentAmount = totalFee;
        }
        
        com.skillsphere.payment.dto.PaymentRequest payReq = new com.skillsphere.payment.dto.PaymentRequest();
        payReq.setStudentId(student.getId());
        payReq.setCourseId(courseId);
        payReq.setAmount(paymentAmount);
        payReq.setPaymentMethod(paymentMethod.toUpperCase());
        payReq.setEmiCount(emiCount);
        payReq.setInstallmentNumber(1);
        payReq.setStatus("PENDING");
        com.skillsphere.payment.dto.PaymentResponse savedPay = paymentService.recordPayment(payReq);
        
        com.skillsphere.batch.dto.EnrollmentResponse enrollRes = batchService.mapToEnrollmentResponse(enrollment);
        enrollRes.setPendingPaymentId(savedPay.getId());
        return ResponseEntity.ok(enrollRes);
    }

    @GetMapping("/courses/{courseId}/batches")
    public ResponseEntity<List<com.skillsphere.batch.dto.BatchResponse>> getCourseActiveBatches(@PathVariable Long courseId) {
        return ResponseEntity.ok(batchService.getActiveBatchesByCourseId(courseId));
    }

    @GetMapping("/courses")
    public ResponseEntity<List<com.skillsphere.course.dto.CourseResponse>> getCourses() {
        return ResponseEntity.ok(courseService.getAllCourses("", org.springframework.data.domain.Pageable.unpaged()).getContent());
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        if (!response.getStudentId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to payment details");
        }
        return ResponseEntity.ok(response);
    }

    @org.springframework.transaction.annotation.Transactional
    @PostMapping("/payments/{paymentId}/pay-mock")
    public ResponseEntity<PaymentResponse> payMock(
            @PathVariable Long paymentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Student student = studentRepository.findByUserId(getStudentUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Student profile not found"));
        PaymentResponse payDetail = paymentService.getPaymentById(paymentId);
        if (!payDetail.getStudentId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to payment");
        }
        PaymentResponse response = paymentService.updatePaymentStatus(paymentId, com.skillsphere.common.enums.PaymentStatus.PAID);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<Map<String, String>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        com.skillsphere.notification.entity.Notification notification = notificationService.getNotificationById(id);
        if (!notification.getUser().getId().equals(getStudentUserId(userDetails))) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthorized access to notification");
        }
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(Map.of("message", "Notification deleted successfully"));
    }

    @PostMapping(value = "/assignments/{assignmentId}/submit-doc", consumes = "multipart/form-data")
    public ResponseEntity<AssignmentSubmissionResponse> submitAssignmentFile(
            @PathVariable Long assignmentId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws java.io.IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        return ResponseEntity.ok(assignmentService.submitAssignmentFile(assignmentId, getStudentUserId(userDetails), file.getOriginalFilename(), file.getBytes()));
    }

    @PostMapping(value = "/projects/{projectId}/submit-doc", consumes = "multipart/form-data")
    public ResponseEntity<ProjectSubmissionResponse> submitProjectFile(
            @PathVariable Long projectId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) throws java.io.IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        return ResponseEntity.ok(projectService.submitProjectFile(projectId, getStudentUserId(userDetails), file.getOriginalFilename(), file.getBytes()));
    }

    @GetMapping("/courses/{courseId}/materials")
    public ResponseEntity<List<com.skillsphere.course.dto.CourseMaterialResponse>> getMaterials(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseMaterialService.getCourseMaterials(courseId));
    }

    @GetMapping("/batches/{batchId}/meetings")
    public ResponseEntity<List<com.skillsphere.batch.dto.MeetingResponse>> getMeetings(@PathVariable Long batchId) {
        return ResponseEntity.ok(meetingService.getBatchMeetings(batchId));
    }
}
