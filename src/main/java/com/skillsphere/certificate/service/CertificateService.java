package com.skillsphere.certificate.service;

import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.certificate.dto.CertificateResponse;
import com.skillsphere.certificate.entity.Certificate;
import com.skillsphere.certificate.repository.CertificateRepository;
import com.skillsphere.common.service.AuditLogService;
import com.skillsphere.common.service.EmailService;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.evaluation.entity.Evaluation;
import com.skillsphere.evaluation.repository.EvaluationRepository;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final TrainerRepository trainerRepository;
    private final AttendanceService attendanceService;
    private final EvaluationRepository evaluationRepository;
    private final com.skillsphere.exam.repository.ExamResultRepository examResultRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public CertificateResponse issueCertificateByTrainer(Long trainerUserId, Long batchId, Long studentId, String grade, Double customScore, String feedback) {
        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer profile not found for user: " + trainerUserId));

        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));

        if (batch.getTrainer() == null || !batch.getTrainer().getId().equals(trainer.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not assigned to manage this batch.");
        }

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Course course = batch.getCourse();
        if (course == null) {
            throw new BadRequestException("Batch is not associated with any course.");
        }

        if (certificateRepository.existsByStudentIdAndCourseId(studentId, course.getId())) {
            throw new DuplicateResourceException("Certificate has already been issued to this student for " + course.getName());
        }

        // 1. Validate Attendance Criteria (>= 70%)
        double attendancePct = attendanceService.getStudentAttendancePercentage(studentId, batchId);
        if (attendancePct < 70.0) {
            throw new BadRequestException(String.format("Student attendance (%.1f%%) is below the required 70%% threshold for certificate issuance.", attendancePct));
        }

        // 2. Validate Exam Score Criteria (Pass >= 50%)
        List<com.skillsphere.exam.entity.ExamResult> examResults = examResultRepository.findByStudentId(studentId);
        double examScoreVal = examResults.isEmpty() ?
                (customScore != null ? customScore : 75.0) :
                examResults.stream().mapToDouble(er -> er.getPercentage() != null ? er.getPercentage() : 0.0).average().orElse(75.0);

        if (examScoreVal < 50.0) {
            throw new BadRequestException(String.format("Student exam average (%.1f%%) is below the passing criteria (50%%).", examScoreVal));
        }

        double finalScore = (customScore != null && customScore > 0) ? customScore : examScoreVal;
        String finalGrade = (grade != null && !grade.trim().isEmpty()) ? grade.trim() : calculateGrade(finalScore);

        // Update or create evaluation record
        Evaluation eval = evaluationRepository.findByStudentIdAndBatchId(studentId, batchId)
                .stream().findFirst()
                .orElse(Evaluation.builder().student(student).batch(batch).build());

        eval.setTrainer(trainer);
        eval.setAttendanceScore(attendancePct);
        eval.setTechnicalScore(finalScore);
        eval.setCommunicationScore(finalScore);
        eval.setAssignmentScore(finalScore);
        eval.setProjectScore(finalScore);
        eval.setOverallScore(finalScore);
        eval.setGrade(finalGrade);
        if (feedback != null && !feedback.trim().isEmpty()) {
            eval.setFeedback(feedback.trim());
        }
        evaluationRepository.save(eval);

        // Generate Certificate
        String rawUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String certNum = "CERT-" + course.getId() + "-" + rawUuid;
        String verificationCode = "SKILL-VERIFY-" + course.getId() + "-" + rawUuid;

        Certificate certificate = Certificate.builder()
                .certificateNumber(certNum)
                .verificationCode(verificationCode)
                .student(student)
                .course(course)
                .issueDate(LocalDate.now())
                .score(finalScore)
                .grade(finalGrade)
                .trainerId(trainer.getId())
                .trainerName(trainer.getUser().getName())
                .status("ISSUED")
                .build();

        certificate = certificateRepository.save(certificate);

        // Notify Student
        notificationService.createNotification(student.getUser(), "Certificate Awarded!",
                String.format("Congratulations! Trainer %s has awarded your Certificate of Completion for '%s' with Grade %s (Score: %.1f%%). Credential ID: %s",
                        trainer.getUser().getName(), course.getName(), finalGrade, finalScore, certNum), "CERTIFICATE");

        // Send Email
        emailService.sendCertificateNotificationEmail(student.getUser().getEmail(), student.getUser().getName(),
                course.getName(), certNum, verificationCode, finalGrade, finalScore);

        // Audit Log
        auditLogService.log("CERTIFICATE_ISSUE_TRAINER", "Certificate", certificate.getId().toString(),
                trainer.getUser().getId(), trainer.getUser().getEmail(), "ROLE_TRAINER",
                String.format("Trainer %s issued Certificate %s (Grade: %s, Score: %.1f%%) for %s to %s",
                        trainer.getUser().getName(), certNum, finalGrade, finalScore, course.getName(), student.getUser().getName()), null);

        return mapToResponse(certificate);
    }

    @Transactional
    public CertificateResponse generateCertificate(Long studentId, Long courseId, Double score) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        if (certificateRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
            throw new DuplicateResourceException("Certificate already issued for this course");
        }

        double finalScore = (score != null) ? score : 85.0;
        String finalGrade = calculateGrade(finalScore);

        String rawUuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String certNum = "CERT-" + course.getId() + "-" + rawUuid;
        String verificationCode = "SKILL-VERIFY-" + course.getId() + "-" + rawUuid;

        Certificate certificate = Certificate.builder()
                .certificateNumber(certNum)
                .verificationCode(verificationCode)
                .student(student)
                .course(course)
                .issueDate(LocalDate.now())
                .score(finalScore)
                .grade(finalGrade)
                .status("ISSUED")
                .build();

        certificate = certificateRepository.save(certificate);

        // Notify student in-app
        notificationService.createNotification(student.getUser(), "Certificate Issued!",
                "Congratulations! Your certificate for course '" + course.getName() + "' is ready. Certificate No: " + certNum + ", Verification Code: " + verificationCode, "CERTIFICATE");

        // Send email
        emailService.sendCertificateNotificationEmail(student.getUser().getEmail(), student.getUser().getName(),
                course.getName(), certNum, verificationCode, finalGrade, finalScore);

        // Audit Log
        auditLogService.log("CERTIFICATE_ISSUE", "Certificate", certificate.getId().toString(),
                student.getUser().getId(), student.getUser().getEmail(), "ROLE_ADMIN",
                "Issued Certificate " + certNum + " for " + course.getName() + " to " + student.getUser().getName(), null);

        return mapToResponse(certificate);
    }

    @Transactional(readOnly = true)
    public CertificateResponse verifyCertificate(String codeOrNumber) {
        if (codeOrNumber == null || codeOrNumber.trim().isEmpty()) {
            throw new BadRequestException("Verification code or certificate number is required");
        }
        String trimmed = codeOrNumber.trim();
        Certificate c = certificateRepository.findByVerificationCode(trimmed)
                .or(() -> certificateRepository.findByCertificateNumber(trimmed))
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with verification query: " + trimmed));
        return mapToResponse(c);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getStudentCertificates(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return certificateRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getAllCertificates() {
        return certificateRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String calculateGrade(double score) {
        if (score >= 90.0) return "A+ Distinction";
        if (score >= 80.0) return "A Excellent";
        if (score >= 70.0) return "B Very Good";
        if (score >= 60.0) return "C Good";
        if (score >= 50.0) return "Pass";
        return "Fail";
    }

    private CertificateResponse mapToResponse(Certificate c) {
        return CertificateResponse.builder()
                .id(c.getId())
                .certificateNumber(c.getCertificateNumber())
                .verificationCode(c.getVerificationCode() != null ? c.getVerificationCode() : c.getCertificateNumber())
                .studentId(c.getStudent().getId())
                .studentName(c.getStudent().getUser().getName())
                .registrationNumber(c.getStudent().getRegistrationNumber())
                .courseId(c.getCourse().getId())
                .courseName(c.getCourse().getName())
                .issueDate(c.getIssueDate())
                .score(c.getScore())
                .grade(c.getGrade() != null ? c.getGrade() : calculateGrade(c.getScore() != null ? c.getScore() : 0.0))
                .trainerId(c.getTrainerId())
                .trainerName(c.getTrainerName())
                .status(c.getStatus())
                .build();
    }
}
