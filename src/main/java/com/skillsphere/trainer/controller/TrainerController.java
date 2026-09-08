package com.skillsphere.trainer.controller;

import com.skillsphere.assignment.dto.*;
import com.skillsphere.assignment.service.AssignmentService;
import com.skillsphere.attendance.dto.AttendanceBulkRequest;
import com.skillsphere.attendance.dto.AttendanceResponse;
import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.batch.service.BatchService;
import com.skillsphere.dashboard.dto.TrainerDashboardResponse;
import com.skillsphere.dashboard.service.DashboardService;
import com.skillsphere.evaluation.dto.EvaluationRequest;
import com.skillsphere.evaluation.dto.EvaluationResponse;
import com.skillsphere.evaluation.service.EvaluationService;
import com.skillsphere.exam.dto.*;
import com.skillsphere.exam.service.ExamService;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.project.dto.*;
import com.skillsphere.project.service.ProjectService;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trainer")
@RequiredArgsConstructor
public class TrainerController {

    private final DashboardService dashboardService;
    private final BatchService batchService;
    private final AttendanceService attendanceService;
    private final AssignmentService assignmentService;
    private final ExamService examService;
    private final ProjectService projectService;
    private final EvaluationService evaluationService;
    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final com.skillsphere.batch.service.MeetingService meetingService;
    private final com.skillsphere.common.service.DocumentTextExtractionService documentTextExtractionService;
    private final com.skillsphere.common.service.KeywordExtractionService keywordExtractionService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.skillsphere.common.service.AuditLogService auditLogService;
    private final com.skillsphere.certificate.service.CertificateService certificateService;

    private Long getTrainerUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("Trainer user not found"));
        return user.getId();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.skillsphere.admin.dto.PasswordChangeRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            throw new BadRequestException("New passwords do not match");
        }

        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new BadRequestException("Trainer user not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Incorrect current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.log("PASSWORD_CHANGE", "User", user.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "Trainer changed password successfully", null);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<TrainerDashboardResponse> getDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(dashboardService.getTrainerDashboard(getTrainerUserId(userDetails)));
    }

    private void validateBatchOwnership(UserDetails userDetails, Long batchId) {
        Trainer trainer = trainerRepository.findByUserId(getTrainerUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Trainer profile not found"));
        com.skillsphere.batch.dto.BatchResponse batch = batchService.getBatchById(batchId);
        if (!trainer.getId().equals(batch.getTrainerId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not authorized to manage this batch");
        }
    }

    @GetMapping("/batches")
    public ResponseEntity<List<BatchResponse>> getAssignedBatches(@AuthenticationPrincipal UserDetails userDetails) {
        Trainer trainer = trainerRepository.findByUserId(getTrainerUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Trainer profile not found"));
        return ResponseEntity.ok(batchService.getTrainerBatches(trainer.getId()));
    }

    @GetMapping("/batches/{batchId}/students")
    public ResponseEntity<List<com.skillsphere.student.dto.StudentResponse>> getBatchStudents(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(batchService.getEnrolledStudents(batchId));
    }

    // Attendance
    @PostMapping("/attendance")
    public ResponseEntity<Map<String, String>> markAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AttendanceBulkRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        attendanceService.markAttendance(request);
        return ResponseEntity.ok(Map.of("message", "Attendance saved successfully"));
    }

    @GetMapping("/batches/{batchId}/attendance")
    public ResponseEntity<List<AttendanceResponse>> getBatchAttendance(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(attendanceService.getBatchAttendance(batchId));
    }

    // Assignments
    @PostMapping("/assignments")
    public ResponseEntity<AssignmentResponse> createAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AssignmentRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(assignmentService.createAssignment(request, getTrainerUserId(userDetails)));
    }

    @GetMapping("/assignments")
    public ResponseEntity<List<AssignmentResponse>> getTrainerAssignments(@AuthenticationPrincipal UserDetails userDetails) {
        Trainer trainer = trainerRepository.findByUserId(getTrainerUserId(userDetails))
                .orElseThrow(() -> new BadRequestException("Trainer profile not found"));
        return ResponseEntity.ok(assignmentService.getAllAssignments().stream()
                .filter(a -> trainer.getId().equals(a.getTrainerId()))
                .toList());
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<List<AssignmentSubmissionResponse>> getAssignmentSubmissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long assignmentId) {
        // Find assignment to check batch ownership
        com.skillsphere.assignment.dto.AssignmentResponse assignment = assignmentService.getAssignmentById(assignmentId);
        validateBatchOwnership(userDetails, assignment.getBatchId());
        return ResponseEntity.ok(assignmentService.getAssignmentSubmissions(assignmentId));
    }

    @PostMapping("/assignments/submissions/{submissionId}/grade")
    public ResponseEntity<AssignmentSubmissionResponse> gradeAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long submissionId,
            @Valid @RequestBody AssignmentEvaluationRequest request) {
        // Since we grade a submission, we check if the submission's assignment's batch belongs to trainer
        com.skillsphere.assignment.dto.AssignmentSubmissionResponse sub = assignmentService.getSubmissionById(submissionId);
        com.skillsphere.assignment.dto.AssignmentResponse assignment = assignmentService.getAssignmentById(sub.getAssignmentId());
        validateBatchOwnership(userDetails, assignment.getBatchId());
        return ResponseEntity.ok(assignmentService.gradeSubmission(submissionId, request));
    }

    // Exams
    @PostMapping("/exams")
    public ResponseEntity<ExamResponse> createExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExamRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(examService.createExam(request));
    }

    @PostMapping("/exams/upload")
    public ResponseEntity<ExamResponse> uploadExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long batchId,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam Integer duration,
            @RequestParam Integer allowedAttempts,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        validateBatchOwnership(userDetails, batchId);
        String content = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok(examService.createExamFromDocument(batchId, title, description, duration, allowedAttempts, content));
    }

    @GetMapping("/batches/{batchId}/exams")
    public ResponseEntity<List<ExamResponse>> getBatchExams(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(examService.getBatchExams(batchId));
    }

    @PostMapping("/exams/{examId}/questions")
    public ResponseEntity<QuestionResponse> addQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId,
            @Valid @RequestBody QuestionRequest request) {
        ExamResponse exam = examService.getExamById(examId);
        validateBatchOwnership(userDetails, exam.getBatchId());
        return ResponseEntity.ok(examService.addQuestion(examId, request));
    }

    @DeleteMapping("/exams/questions/{questionId}")
    public ResponseEntity<Map<String, String>> deleteQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId) {
        // Find exam from question to validate ownership
        Long examId = examService.getExamIdByQuestionId(questionId);
        ExamResponse exam = examService.getExamById(examId);
        validateBatchOwnership(userDetails, exam.getBatchId());
        examService.deleteQuestion(questionId);
        return ResponseEntity.ok(Map.of("message", "Question deleted successfully"));
    }

    @GetMapping("/exams/{examId}/submissions")
    public ResponseEntity<List<ExamResultResponse>> getExamSubmissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long examId) {
        ExamResponse exam = examService.getExamById(examId);
        validateBatchOwnership(userDetails, exam.getBatchId());
        return ResponseEntity.ok(examService.getExamSubmissions(examId));
    }

    // Projects
    @PostMapping("/projects")
    public ResponseEntity<ProjectResponse> createProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ProjectRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(projectService.createProject(request, getTrainerUserId(userDetails)));
    }

    @GetMapping("/batches/{batchId}/projects")
    public ResponseEntity<List<ProjectResponse>> getBatchProjects(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(projectService.getBatchProjects(batchId));
    }

    @GetMapping("/projects/{projectId}/submissions")
    public ResponseEntity<List<ProjectSubmissionResponse>> getProjectSubmissions(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long projectId) {
        com.skillsphere.project.dto.ProjectResponse project = projectService.getProjectById(projectId);
        validateBatchOwnership(userDetails, project.getBatchId());
        return ResponseEntity.ok(projectService.getProjectSubmissions(projectId));
    }

    @PostMapping("/projects/submissions/{submissionId}/grade")
    public ResponseEntity<ProjectSubmissionResponse> gradeProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long submissionId,
            @Valid @RequestBody ProjectEvaluationRequest request) {
        com.skillsphere.project.dto.ProjectSubmissionResponse sub = projectService.getSubmissionById(submissionId);
        com.skillsphere.project.dto.ProjectResponse project = projectService.getProjectById(sub.getProjectId());
        validateBatchOwnership(userDetails, project.getBatchId());
        return ResponseEntity.ok(projectService.gradeProjectSubmission(submissionId, request));
    }

    // Evaluations
    @GetMapping("/batches/{batchId}/evaluations/eligibility")
    public ResponseEntity<List<com.skillsphere.evaluation.dto.EvaluationEligibilityResponse>> getBatchEligibilityList(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(evaluationService.getBatchEligibilityList(batchId));
    }

    @PostMapping("/batches/{batchId}/students/{studentId}/issue-certificate")
    public ResponseEntity<com.skillsphere.certificate.dto.CertificateResponse> issueCertificate(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long batchId,
            @PathVariable Long studentId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) Double score,
            @RequestParam(required = false) String feedback) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(certificateService.issueCertificateByTrainer(
                getTrainerUserId(userDetails), batchId, studentId, grade, score, feedback));
    }

    @PostMapping("/evaluations")
    public ResponseEntity<EvaluationResponse> evaluateStudent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EvaluationRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(evaluationService.evaluateStudent(request, getTrainerUserId(userDetails)));
    }

    @GetMapping("/evaluations")
    public ResponseEntity<List<EvaluationResponse>> getTrainerEvaluations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(evaluationService.getTrainerEvaluations(getTrainerUserId(userDetails)));
    }

    @GetMapping("/evaluations/draft")
    public ResponseEntity<EvaluationRequest> getEvaluationDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long studentId,
            @RequestParam Long batchId) {
        validateBatchOwnership(userDetails, batchId);
        return ResponseEntity.ok(evaluationService.getEvaluationDraft(studentId, batchId));
    }

    @PostMapping("/meetings")
    public ResponseEntity<com.skillsphere.batch.dto.MeetingResponse> scheduleMeeting(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody com.skillsphere.batch.dto.MeetingRequest request) {
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(meetingService.scheduleMeeting(request));
    }

    @PostMapping("/assignments/extract-keywords")
    public ResponseEntity<Map<String, Object>> extractAssignmentKeywords(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        String content = documentTextExtractionService.extractText(file.getOriginalFilename(), file.getBytes());
        List<String> keywords = keywordExtractionService.extractKeywords(content);
        return ResponseEntity.ok(Map.of("text", content, "keywords", keywords));
    }

    @PostMapping("/projects/extract-keywords")
    public ResponseEntity<Map<String, Object>> extractProjectKeywords(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) throws Exception {
        String content = documentTextExtractionService.extractText(file.getOriginalFilename(), file.getBytes());
        List<String> keywords = keywordExtractionService.extractKeywords(content);
        return ResponseEntity.ok(Map.of("text", content, "keywords", keywords));
    }

    @PutMapping("/assignments/{id}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody AssignmentRequest request) {
        AssignmentResponse assignment = assignmentService.getAssignmentById(id);
        validateBatchOwnership(userDetails, assignment.getBatchId());
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(assignmentService.updateAssignment(id, request));
    }

    @DeleteMapping("/assignments/{id}")
    public ResponseEntity<Map<String, String>> deleteAssignment(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        AssignmentResponse assignment = assignmentService.getAssignmentById(id);
        validateBatchOwnership(userDetails, assignment.getBatchId());
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok(Map.of("message", "Assignment deleted successfully"));
    }

    @PutMapping("/projects/{id}")
    public ResponseEntity<ProjectResponse> updateProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ProjectRequest request) {
        ProjectResponse project = projectService.getProjectById(id);
        validateBatchOwnership(userDetails, project.getBatchId());
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    @DeleteMapping("/projects/{id}")
    public ResponseEntity<Map<String, String>> deleteProject(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ProjectResponse project = projectService.getProjectById(id);
        validateBatchOwnership(userDetails, project.getBatchId());
        projectService.deleteProject(id);
        return ResponseEntity.ok(Map.of("message", "Project deleted successfully"));
    }

    @PutMapping("/exams/{id}")
    public ResponseEntity<ExamResponse> updateExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ExamRequest request) {
        ExamResponse exam = examService.getExamById(id);
        validateBatchOwnership(userDetails, exam.getBatchId());
        validateBatchOwnership(userDetails, request.getBatchId());
        return ResponseEntity.ok(examService.updateExam(id, request));
    }

    @DeleteMapping("/exams/{id}")
    public ResponseEntity<Map<String, String>> deleteExam(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        ExamResponse exam = examService.getExamById(id);
        validateBatchOwnership(userDetails, exam.getBatchId());
        examService.deleteExam(id);
        return ResponseEntity.ok(Map.of("message", "Exam deleted successfully"));
    }

    @PutMapping("/exams/questions/{questionId}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionRequest request) {
        Long examId = examService.getExamIdByQuestionId(questionId);
        ExamResponse exam = examService.getExamById(examId);
        validateBatchOwnership(userDetails, exam.getBatchId());
        return ResponseEntity.ok(examService.updateQuestion(questionId, request));
    }
}
