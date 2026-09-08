package com.skillsphere.admin.controller;

import com.skillsphere.admin.dto.PasswordChangeRequest;
import com.skillsphere.batch.dto.BatchRequest;
import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.batch.dto.EnrollmentResponse;
import com.skillsphere.batch.service.BatchService;
import com.skillsphere.course.dto.CourseRequest;
import com.skillsphere.course.dto.CourseResponse;
import com.skillsphere.course.service.CourseService;
import com.skillsphere.dashboard.dto.AdminDashboardResponse;
import com.skillsphere.dashboard.service.DashboardService;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.payment.dto.PaymentRequest;
import com.skillsphere.payment.dto.PaymentResponse;
import com.skillsphere.payment.service.PaymentService;
import com.skillsphere.student.dto.StudentRequest;
import com.skillsphere.student.dto.StudentResponse;
import com.skillsphere.student.service.StudentService;
import com.skillsphere.trainer.dto.TrainerRequest;
import com.skillsphere.trainer.dto.TrainerResponse;
import com.skillsphere.trainer.service.TrainerService;
import com.skillsphere.certificate.dto.CertificateResponse;
import com.skillsphere.certificate.service.CertificateService;
import com.skillsphere.evaluation.dto.EvaluationResponse;
import com.skillsphere.evaluation.service.EvaluationService;
import com.skillsphere.attendance.dto.AttendanceResponse;
import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final DashboardService dashboardService;
    private final StudentService studentService;
    private final TrainerService trainerService;
    private final CourseService courseService;
    private final BatchService batchService;
    private final PaymentService paymentService;
    private final CertificateService certificateService;
    private final EvaluationService evaluationService;
    private final AttendanceService attendanceService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.skillsphere.course.service.CourseMaterialService courseMaterialService;
    private final com.skillsphere.common.service.AuditLogService auditLogService;
    private final com.skillsphere.notification.service.AnnouncementService announcementService;

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    // Students CRUD
    @GetMapping("/students")
    public ResponseEntity<Page<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(studentService.getAllStudents(search, pageable));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PostMapping("/students")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.createStudent(request));
    }

    @PutMapping("/students/{id}")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Long id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @DeleteMapping("/students/{id}")
    public ResponseEntity<Map<String, String>> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok(Map.of("message", "Student deleted successfully"));
    }

    // Trainers CRUD
    @GetMapping("/trainers")
    public ResponseEntity<Page<TrainerResponse>> getAllTrainers(@PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(trainerService.getAllTrainers(pageable));
    }

    @GetMapping("/trainers/{id}")
    public ResponseEntity<TrainerResponse> getTrainerById(@PathVariable Long id) {
        return ResponseEntity.ok(trainerService.getTrainerById(id));
    }

    @PostMapping("/trainers")
    public ResponseEntity<TrainerResponse> createTrainer(@Valid @RequestBody TrainerRequest request) {
        return ResponseEntity.ok(trainerService.createTrainer(request));
    }

    @PutMapping("/trainers/{id}")
    public ResponseEntity<TrainerResponse> updateTrainer(
            @PathVariable Long id,
            @Valid @RequestBody TrainerRequest request) {
        return ResponseEntity.ok(trainerService.updateTrainer(id, request));
    }

    @DeleteMapping("/trainers/{id}")
    public ResponseEntity<Map<String, String>> deleteTrainer(@PathVariable Long id) {
        trainerService.deleteTrainer(id);
        return ResponseEntity.ok(Map.of("message", "Trainer deleted successfully"));
    }

    // Courses CRUD
    @GetMapping("/courses")
    public ResponseEntity<Page<CourseResponse>> getAllCourses(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(courseService.getAllCourses(search, pageable));
    }

    @GetMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @PostMapping("/courses")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.createCourse(request));
    }

    @PutMapping("/courses/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody CourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Map<String, String>> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(Map.of("message", "Course deleted successfully"));
    }

    // Batches CRUD
    @GetMapping("/batches")
    public ResponseEntity<List<BatchResponse>> getAllBatches() {
        return ResponseEntity.ok(batchService.getAllBatches());
    }

    @GetMapping("/batches/{id}")
    public ResponseEntity<BatchResponse> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(batchService.getBatchById(id));
    }

    @PostMapping("/batches")
    public ResponseEntity<BatchResponse> createBatch(@Valid @RequestBody BatchRequest request) {
        return ResponseEntity.ok(batchService.createBatch(request));
    }

    @PutMapping("/batches/{id}")
    public ResponseEntity<BatchResponse> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody BatchRequest request) {
        return ResponseEntity.ok(batchService.updateBatch(id, request));
    }

    @DeleteMapping("/batches/{id}")
    public ResponseEntity<Map<String, String>> deleteBatch(@PathVariable Long id) {
        batchService.deleteBatch(id);
        return ResponseEntity.ok(Map.of("message", "Batch deleted successfully"));
    }

    // Enrollment in batch
    @GetMapping("/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> getAllEnrollments(@RequestParam(required = false) Long courseId) {
        return ResponseEntity.ok(batchService.getEnrollments(courseId));
    }

    @PostMapping("/batches/{batchId}/students/{studentId}")
    public ResponseEntity<EnrollmentResponse> enrollStudentInBatch(
            @PathVariable Long batchId,
            @PathVariable Long studentId) {
        return ResponseEntity.ok(batchService.enrollStudent(batchId, studentId));
    }

    @DeleteMapping("/batches/{batchId}/students/{studentId}")
    public ResponseEntity<Map<String, String>> removeStudentFromBatch(
            @PathVariable Long batchId,
            @PathVariable Long studentId) {
        batchService.removeStudent(batchId, studentId);
        return ResponseEntity.ok(Map.of("message", "Student removed from batch successfully"));
    }

    @GetMapping("/batches/{batchId}/students")
    public ResponseEntity<List<StudentResponse>> getEnrolledStudents(@PathVariable Long batchId) {
        return ResponseEntity.ok(batchService.getEnrolledStudents(batchId));
    }

    // Payments
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/payments")
    public ResponseEntity<PaymentResponse> recordPayment(@Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.recordPayment(request));
    }

    @PutMapping("/payments/{id}/status")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, com.skillsphere.common.enums.PaymentStatus.valueOf(status.toUpperCase())));
    }

    // Admin password change
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PasswordChangeRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("Admin not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log("PASSWORD_CHANGE", "User", user.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "Admin changed password successfully", null);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    // ==========================================
    // USER MANAGEMENT
    // ==========================================
    @GetMapping("/users")
    public ResponseEntity<Page<Map<String, Object>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @PageableDefault(size = 10) Pageable pageable) {
        
        Page<User> users;
        if (role != null && !role.trim().isEmpty() && !"ALL".equalsIgnoreCase(role)) {
            try {
                com.skillsphere.common.enums.Role r = com.skillsphere.common.enums.Role.valueOf(role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase());
                users = userRepository.findAll(pageable); // Can be filtered
            } catch(Exception e) {
                users = userRepository.findAll(pageable);
            }
        } else {
            users = userRepository.findAll(pageable);
        }

        Page<Map<String, Object>> response = users.map(u -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRole() != null ? u.getRole().name() : "ROLE_STUDENT");
            map.put("active", u.isActive());
            map.put("createdAt", u.getCreatedAt());
            return map;
        });

        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<Map<String, Object>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active,
            @AuthenticationPrincipal UserDetails userDetails) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + id));

        targetUser.setActive(active);
        userRepository.save(targetUser);

        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (admin != null) {
            auditLogService.log("USER_STATUS_CHANGE", "User", id.toString(),
                    admin.getId(), admin.getEmail(), admin.getRole().name(),
                    "Changed active status of " + targetUser.getEmail() + " to " + active, null);
        }

        return ResponseEntity.ok(Map.of("message", "User status updated to " + (active ? "ACTIVE" : "INACTIVE"), "active", active));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Map<String, Object>> changeUserRole(
            @PathVariable Long id,
            @RequestParam String role,
            @AuthenticationPrincipal UserDetails userDetails) {
        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found with id: " + id));

        String formattedRole = role.toUpperCase().startsWith("ROLE_") ? role.toUpperCase() : "ROLE_" + role.toUpperCase();
        com.skillsphere.common.enums.Role newRole = com.skillsphere.common.enums.Role.valueOf(formattedRole);
        com.skillsphere.common.enums.Role oldRole = targetUser.getRole();
        targetUser.setRole(newRole);
        userRepository.save(targetUser);

        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        if (admin != null) {
            auditLogService.log("USER_ROLE_CHANGE", "User", id.toString(),
                    admin.getId(), admin.getEmail(), admin.getRole().name(),
                    "Changed role of " + targetUser.getEmail() + " from " + oldRole + " to " + newRole, null);
        }

        return ResponseEntity.ok(Map.of("message", "User role updated successfully to " + newRole, "role", newRole.name()));
    }

    // ==========================================
    // AUDIT LOGS
    // ==========================================
    @GetMapping("/audit-logs")
    public ResponseEntity<Page<com.skillsphere.common.dto.AuditLogResponse>> getAuditLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userRole,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @PageableDefault(size = 15, sort = "timestamp", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getFilteredAuditLogs(search, action, userRole, entityType, startDate, endDate, pageable));
    }

    @GetMapping("/audit-logs/stats")
    public ResponseEntity<Map<String, Object>> getAuditStats() {
        return ResponseEntity.ok(auditLogService.getAuditStats());
    }

    // ==========================================
    // ANNOUNCEMENTS MANAGEMENT
    // ==========================================
    @GetMapping("/announcements")
    public ResponseEntity<Page<com.skillsphere.notification.dto.AnnouncementResponse>> getAllAnnouncements(
            @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(announcementService.getAllAnnouncements(pageable));
    }

    @PostMapping("/announcements")
    public ResponseEntity<com.skillsphere.notification.dto.AnnouncementResponse> createAnnouncement(
            @Valid @RequestBody com.skillsphere.notification.dto.AnnouncementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        return ResponseEntity.ok(announcementService.createAnnouncement(request, admin != null ? admin.getId() : null));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<com.skillsphere.notification.dto.AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @Valid @RequestBody com.skillsphere.notification.dto.AnnouncementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request, admin != null ? admin.getId() : null));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<Map<String, String>> deleteAnnouncement(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername()).orElse(null);
        announcementService.deleteAnnouncement(id, admin != null ? admin.getId() : null);
        return ResponseEntity.ok(Map.of("message", "Announcement deleted successfully"));
    }

    // Certificates
    @PostMapping({"/certificates", "/certificates/generate"})
    public ResponseEntity<CertificateResponse> generateCertificate(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam(required = false) Double score) {
        return ResponseEntity.ok(certificateService.generateCertificate(studentId, courseId, score));
    }

    @GetMapping("/certificates")
    public ResponseEntity<List<CertificateResponse>> getAllCertificates() {
        return ResponseEntity.ok(certificateService.getAllCertificates());
    }

    // Evaluations
    @GetMapping("/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getAllEvaluations() {
        return ResponseEntity.ok(evaluationService.getAllEvaluations());
    }

    // Attendance
    @GetMapping({"/batches/{batchId}/attendance", "/attendance/batches/{batchId}"})
    public ResponseEntity<List<AttendanceResponse>> getBatchAttendance(@PathVariable Long batchId) {
        return ResponseEntity.ok(attendanceService.getBatchAttendance(batchId));
    }

    @GetMapping("/attendance")
    public ResponseEntity<List<AttendanceResponse>> getAttendance(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long studentId) {
        if (batchId != null) {
            return ResponseEntity.ok(attendanceService.getBatchAttendance(batchId));
        } else if (studentId != null) {
            return ResponseEntity.ok(attendanceService.getStudentAttendance(studentId, null));
        }
        return ResponseEntity.ok(List.of());
    }

    // Eligible students for a course
    @GetMapping("/courses/{courseId}/eligible-students")
    public ResponseEntity<List<com.skillsphere.student.dto.StudentResponse>> getEligibleStudents(@PathVariable Long courseId) {
        return ResponseEntity.ok(batchService.getEligibleStudentsForCourse(courseId));
    }

    // Learning materials
    @PostMapping("/courses/{courseId}/materials")
    public ResponseEntity<com.skillsphere.course.dto.CourseMaterialResponse> addMaterial(
            @PathVariable Long courseId,
            @Valid @RequestBody com.skillsphere.course.dto.CourseMaterialRequest request) {
        return ResponseEntity.ok(courseMaterialService.addMaterial(courseId, request));
    }

    @GetMapping("/courses/{courseId}/materials")
    public ResponseEntity<List<com.skillsphere.course.dto.CourseMaterialResponse>> getMaterials(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseMaterialService.getCourseMaterials(courseId));
    }

    @DeleteMapping("/courses/materials/{id}")
    public ResponseEntity<Map<String, String>> deleteMaterial(@PathVariable Long id) {
        courseMaterialService.deleteMaterial(id);
        return ResponseEntity.ok(Map.of("message", "Course material deleted successfully"));
    }
}
