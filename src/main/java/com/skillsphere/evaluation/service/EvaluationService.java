package com.skillsphere.evaluation.service;

import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.evaluation.dto.EvaluationRequest;
import com.skillsphere.evaluation.dto.EvaluationResponse;
import com.skillsphere.evaluation.entity.Evaluation;
import com.skillsphere.evaluation.repository.EvaluationRepository;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final StudentRepository studentRepository;
    private final TrainerRepository trainerRepository;
    private final BatchRepository batchRepository;
    private final NotificationService notificationService;
    private final com.skillsphere.exam.repository.ExamResultRepository examResultRepository;
    private final com.skillsphere.assignment.repository.AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final com.skillsphere.project.repository.ProjectSubmissionRepository projectSubmissionRepository;
    private final com.skillsphere.batch.repository.EnrollmentRepository enrollmentRepository;
    private final com.skillsphere.certificate.repository.CertificateRepository certificateRepository;
    private final com.skillsphere.attendance.service.AttendanceService attendanceService;

    @Transactional(readOnly = true)
    public List<com.skillsphere.evaluation.dto.EvaluationEligibilityResponse> getBatchEligibilityList(Long batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));
        Long courseId = batch.getCourse() != null ? batch.getCourse().getId() : null;
        String courseName = batch.getCourse() != null ? batch.getCourse().getName() : "Unassigned";

        List<com.skillsphere.batch.entity.Enrollment> enrollments = enrollmentRepository.findByBatchId(batchId).stream()
                .filter(e -> e.getStatus() == com.skillsphere.common.enums.EnrollmentStatus.ACTIVE)
                .collect(Collectors.toList());

        return enrollments.stream().map(e -> {
            Student student = e.getStudent();
            Long studentId = student.getId();

            // 1. Attendance
            Map<String, Object> attSummary = attendanceService.getStudentAttendanceSummary(studentId, batchId);
            long totalClasses = (attSummary.get("totalClasses") instanceof Number) ? ((Number) attSummary.get("totalClasses")).longValue() : 0;
            long present = (attSummary.get("present") instanceof Number) ? ((Number) attSummary.get("present")).longValue() : 0;
            long late = (attSummary.get("late") instanceof Number) ? ((Number) attSummary.get("late")).longValue() : 0;
            double attPct = (attSummary.get("percentage") instanceof Number) ? ((Number) attSummary.get("percentage")).doubleValue() : 0.0;

            // 2. Exams
            List<com.skillsphere.exam.entity.ExamResult> examResults = examResultRepository.findByStudentId(studentId).stream()
                    .filter(er -> er.getExam() != null && er.getExam().getBatch() != null && er.getExam().getBatch().getId().equals(batchId))
                    .collect(Collectors.toList());
            int examCount = examResults.size();
            double avgExam = examResults.isEmpty() ? 0.0 : examResults.stream()
                    .mapToDouble(er -> er.getPercentage() != null ? er.getPercentage() : 0.0)
                    .average().orElse(0.0);
            boolean examPassed = examCount > 0 ? (avgExam >= 50.0) : true; // If no exams yet, allow based on overall evaluation

            // 3. Assignments & Projects
            List<com.skillsphere.assignment.entity.AssignmentSubmission> assigns = assignmentSubmissionRepository.findByStudentId(studentId);
            double avgAssign = assigns.isEmpty() ? 0.0 : assigns.stream()
                    .filter(a -> a.getMarks() != null)
                    .mapToDouble(a -> a.getMarks().doubleValue())
                    .average().orElse(0.0);

            List<com.skillsphere.project.entity.ProjectSubmission> projs = projectSubmissionRepository.findByStudentId(studentId);
            double avgProj = projs.isEmpty() ? 0.0 : projs.stream()
                    .filter(p -> p.getMarks() != null)
                    .mapToDouble(p -> p.getMarks().doubleValue())
                    .average().orElse(0.0);

            // 4. Overall score
            double overall = (avgExam * 0.4) + (attPct * 0.3) + (avgAssign * 0.15) + (avgProj * 0.15);
            if (overall == 0.0 && attPct > 0) {
                overall = attPct;
            }

            String suggestedGrade = (overall >= 90.0) ? "A+ Distinction" :
                    (overall >= 80.0) ? "A Excellent" :
                    (overall >= 70.0) ? "B Very Good" :
                    (overall >= 60.0) ? "C Good" :
                    (overall >= 50.0) ? "Pass" : "Fail";

            // 5. Certificate Status
            java.util.Optional<com.skillsphere.certificate.entity.Certificate> certOpt = (courseId != null) ?
                    certificateRepository.findByStudentIdAndCourseId(studentId, courseId) : java.util.Optional.empty();

            boolean certIssued = certOpt.isPresent();
            String certNum = certIssued ? certOpt.get().getCertificateNumber() : null;
            String verifyCode = certIssued ? certOpt.get().getVerificationCode() : null;
            String grade = certIssued ? certOpt.get().getGrade() : null;

            boolean eligible = !certIssued && (attPct >= 70.0) && (avgExam >= 50.0 || examCount == 0);
            String reason;
            if (certIssued) {
                reason = "Certificate Issued (" + certNum + ")";
            } else if (attPct < 70.0 && (examCount > 0 && avgExam < 50.0)) {
                reason = String.format("Attendance (%.1f%% < 70%%) and Exam (%.1f%% < 50%%) below criteria", attPct, avgExam);
            } else if (attPct < 70.0) {
                reason = String.format("Attendance (%.1f%%) is below 70%% requirement", attPct);
            } else if (examCount > 0 && avgExam < 50.0) {
                reason = String.format("Exam score (%.1f%%) is below 50%% passing score", avgExam);
            } else {
                reason = String.format("Eligible (Attendance: %.1f%%, Exam: %.1f%%)", attPct, avgExam);
            }

            return com.skillsphere.evaluation.dto.EvaluationEligibilityResponse.builder()
                    .studentId(studentId)
                    .studentName(student.getUser().getName())
                    .email(student.getUser().getEmail())
                    .registrationNumber(student.getRegistrationNumber())
                    .batchId(batchId)
                    .batchName(batch.getBatchName())
                    .courseId(courseId)
                    .courseName(courseName)
                    .attendancePercentage(Math.round(attPct * 100.0) / 100.0)
                    .totalClasses(totalClasses)
                    .attendedClasses(present + late)
                    .examAverageScore(Math.round(avgExam * 100.0) / 100.0)
                    .examPassed(examPassed)
                    .examCount(examCount)
                    .assignmentScore(Math.round(avgAssign * 100.0) / 100.0)
                    .projectScore(Math.round(avgProj * 100.0) / 100.0)
                    .overallScore(Math.round(overall * 100.0) / 100.0)
                    .suggestedGrade(suggestedGrade)
                    .eligibleForCertificate(eligible)
                    .eligibilityReason(reason)
                    .certificateIssued(certIssued)
                    .certificateNumber(certNum)
                    .verificationCode(verifyCode)
                    .grade(grade)
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EvaluationRequest getEvaluationDraft(Long studentId, Long batchId) {
        Map<String, Object> attSummary = attendanceService.getStudentAttendanceSummary(studentId, batchId);
        Double attPct = (Double) attSummary.get("percentage");

        List<com.skillsphere.exam.entity.ExamResult> examResults = examResultRepository.findByStudentId(studentId).stream()
                .filter(er -> er.getExam().getBatch().getId().equals(batchId))
                .collect(Collectors.toList());
        double examAvg = examResults.isEmpty() ? 0.0 : examResults.stream()
                .mapToDouble(er -> er.getPercentage() != null ? er.getPercentage() : 0.0)
                .average()
                .orElse(0.0);

        List<com.skillsphere.assignment.entity.AssignmentSubmission> assignSubmissions = assignmentSubmissionRepository.findByStudentId(studentId).stream()
                .filter(as -> as.getAssignment().getBatch().getId().equals(batchId) && as.getMarks() != null)
                .collect(Collectors.toList());
        double assignAvg = assignSubmissions.isEmpty() ? 0.0 : assignSubmissions.stream()
                .mapToDouble(as -> {
                    int max = as.getAssignment().getTotalMarks() != null ? as.getAssignment().getTotalMarks() : 100;
                    return max > 0 ? ((double) as.getMarks() / max) * 100.0 : 0.0;
                })
                .average()
                .orElse(0.0);

        List<com.skillsphere.project.entity.ProjectSubmission> projSubmissions = projectSubmissionRepository.findByStudentId(studentId).stream()
                .filter(ps -> ps.getProject().getBatch().getId().equals(batchId) && ps.getMarks() != null)
                .collect(Collectors.toList());
        double projAvg = projSubmissions.isEmpty() ? 0.0 : projSubmissions.stream()
                .mapToDouble(ps -> {
                    int max = ps.getProject().getTotalMarks() != null ? ps.getProject().getTotalMarks() : 100;
                    return max > 0 ? ((double) ps.getMarks() / max) * 100.0 : 0.0;
                })
                .average()
                .orElse(0.0);

        EvaluationRequest draft = new EvaluationRequest();
        draft.setStudentId(studentId);
        draft.setBatchId(batchId);
        draft.setAttendanceScore(attPct != null ? Math.round(attPct * 100.0) / 100.0 : 0.0);
        draft.setTechnicalScore(Math.round(examAvg * 100.0) / 100.0);
        draft.setAssignmentScore(Math.round(assignAvg * 100.0) / 100.0);
        draft.setProjectScore(Math.round(projAvg * 100.0) / 100.0);
        draft.setCommunicationScore(0.0);
        
        return draft;
    }

    @Transactional
    public EvaluationResponse evaluateStudent(EvaluationRequest request, Long trainerUserId) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.getStudentId()));

        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found for user: " + trainerUserId));

        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        // Calculate overall score as simple average of provided scores
        double count = 0;
        double sum = 0;
        if (request.getTechnicalScore() != null) { sum += request.getTechnicalScore(); count++; }
        if (request.getCommunicationScore() != null) { sum += request.getCommunicationScore(); count++; }
        if (request.getAttendanceScore() != null) { sum += request.getAttendanceScore(); count++; }
        if (request.getAssignmentScore() != null) { sum += request.getAssignmentScore(); count++; }
        if (request.getProjectScore() != null) { sum += request.getProjectScore(); count++; }

        double overallScore = count > 0 ? sum / count : 0.0;

        Evaluation evaluation = Evaluation.builder()
                .student(student)
                .trainer(trainer)
                .batch(batch)
                .technicalScore(request.getTechnicalScore())
                .communicationScore(request.getCommunicationScore())
                .attendanceScore(request.getAttendanceScore())
                .assignmentScore(request.getAssignmentScore())
                .projectScore(request.getProjectScore())
                .overallScore(overallScore)
                .feedback(request.getFeedback())
                .build();

        evaluation = evaluationRepository.save(evaluation);

        // Notify student
        notificationService.createNotification(student.getUser(), "New Evaluation Posted",
                "Trainer " + trainer.getUser().getName() + " posted your overall evaluation. Score: " + String.format("%.2f", overallScore), "EVALUATION");

        return mapToResponse(evaluation);
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getStudentEvaluations(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return evaluationRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getTrainerEvaluations(Long trainerUserId) {
        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found for user: " + trainerUserId));
        return evaluationRepository.findByTrainerId(trainer.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EvaluationResponse> getAllEvaluations() {
        return evaluationRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private EvaluationResponse mapToResponse(Evaluation e) {
        return EvaluationResponse.builder()
                .id(e.getId())
                .studentId(e.getStudent().getId())
                .studentName(e.getStudent().getUser().getName())
                .registrationNumber(e.getStudent().getRegistrationNumber())
                .trainerId(e.getTrainer() != null ? e.getTrainer().getId() : null)
                .trainerName(e.getTrainer() != null ? e.getTrainer().getUser().getName() : "UNASSIGNED")
                .batchId(e.getBatch() != null ? e.getBatch().getId() : null)
                .batchName(e.getBatch() != null ? e.getBatch().getBatchName() : null)
                .technicalScore(e.getTechnicalScore())
                .communicationScore(e.getCommunicationScore())
                .attendanceScore(e.getAttendanceScore())
                .assignmentScore(e.getAssignmentScore())
                .projectScore(e.getProjectScore())
                .overallScore(e.getOverallScore())
                .feedback(e.getFeedback())
                .evaluatedAt(e.getEvaluatedAt())
                .build();
    }
}
