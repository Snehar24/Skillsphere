package com.skillsphere.assignment.service;

import com.skillsphere.assignment.dto.*;
import com.skillsphere.assignment.entity.Assignment;
import com.skillsphere.assignment.entity.AssignmentSubmission;
import com.skillsphere.assignment.repository.AssignmentRepository;
import com.skillsphere.assignment.repository.AssignmentSubmissionRepository;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final BatchRepository batchRepository;
    private final TrainerRepository trainerRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final com.skillsphere.common.service.DocumentTextExtractionService textExtractionService;
    private final com.skillsphere.common.service.KeywordEvaluationService keywordEvaluationService;

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAllAssignments() {
        return assignmentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
        return mapToResponse(assignment);
    }

    @Transactional
    public AssignmentResponse createAssignment(AssignmentRequest request, Long trainerUserId) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found for user: " + trainerUserId));

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .course(batch.getCourse())
                .batch(batch)
                .trainer(trainer)
                .dueDate(request.getDueDate())
                .totalMarks(request.getTotalMarks())
                .trainerContent(request.getTrainerContent())
                .requiredKeywords(request.getRequiredKeywords())
                .status("ACTIVE")
                .build();

        assignment = assignmentRepository.save(assignment);

        // Notify enrolled students
        enrollmentRepository.findByBatchId(batch.getId()).forEach(enrollment -> {
            notificationService.createNotification(enrollment.getStudent().getUser(), "New Assignment Released",
                    "New assignment '" + request.getTitle() + "' is posted. Due on " + request.getDueDate(), "ASSIGNMENT");
        });

        return mapToResponse(assignment);
    }

    @Transactional
    public AssignmentResponse updateAssignment(Long id, AssignmentRequest request) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setDueDate(request.getDueDate());
        assignment.setTotalMarks(request.getTotalMarks());
        assignment.setTrainerContent(request.getTrainerContent());
        assignment.setRequiredKeywords(request.getRequiredKeywords());
        if (request.getStatus() != null) {
            assignment.setStatus(request.getStatus());
        }

        assignment = assignmentRepository.save(assignment);
        return mapToResponse(assignment);
    }

    @Transactional
    public void deleteAssignment(Long id) {
        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", id));
        assignmentRepository.delete(assignment);
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignment(Long assignmentId, Long studentUserId, AssignmentSubmissionRequest request) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        // Create or update submission
        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        submission.setAnswer(request.getAnswer());
        submission.setStatus("SUBMITTED");
        submission.setSubmissionDate(LocalDateTime.now());

        submission = submissionRepository.save(submission);

        if (assignment.getTrainer() != null) {
            notificationService.createNotification(assignment.getTrainer().getUser(), "Assignment Submitted",
                    student.getUser().getName() + " submitted assignment '" + assignment.getTitle() + "'", "ASSIGNMENT");
        }

        return mapToSubmissionResponse(submission);
    }

    @Transactional
    public AssignmentSubmissionResponse gradeSubmission(Long submissionId, AssignmentEvaluationRequest request) {
        AssignmentSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        if (request.getMarks() > submission.getAssignment().getTotalMarks()) {
            throw new BadRequestException("Marks cannot exceed total assignment marks (" + submission.getAssignment().getTotalMarks() + ")");
        }

        submission.setMarks(request.getMarks());
        submission.setFeedback(request.getFeedback());
        submission.setStatus("GRADED");

        submission = submissionRepository.save(submission);

        // Notify student
        notificationService.createNotification(submission.getStudent().getUser(), "Assignment Graded",
                "Your submission for '" + submission.getAssignment().getTitle() + "' has been graded. Marks: " + request.getMarks(), "ASSIGNMENT");

        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public AssignmentSubmissionResponse getSubmissionById(Long id) {
        AssignmentSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", id));
        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getAssignmentSubmissions(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentSubmissionResponse> getStudentSubmissions(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return submissionRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getBatchAssignments(Long batchId) {
        return assignmentRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AssignmentResponse mapToResponse(Assignment assignment) {
        return AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .courseId(assignment.getCourse() != null ? assignment.getCourse().getId() : null)
                .courseName(assignment.getCourse() != null ? assignment.getCourse().getName() : null)
                .batchId(assignment.getBatch() != null ? assignment.getBatch().getId() : null)
                .batchName(assignment.getBatch() != null ? assignment.getBatch().getBatchName() : null)
                .trainerId(assignment.getTrainer() != null ? assignment.getTrainer().getId() : null)
                .trainerName(assignment.getTrainer() != null ? assignment.getTrainer().getUser().getName() : "UNASSIGNED")
                .dueDate(assignment.getDueDate())
                .totalMarks(assignment.getTotalMarks())
                .trainerContent(assignment.getTrainerContent())
                .requiredKeywords(assignment.getRequiredKeywords())
                .status(assignment.getStatus())
                .build();
    }

    private AssignmentSubmissionResponse mapToSubmissionResponse(AssignmentSubmission submission) {
        return AssignmentSubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getUser().getName())
                .answer(submission.getAnswer())
                .marks(submission.getMarks())
                .feedback(submission.getFeedback())
                .status(submission.getStatus())
                .submissionDate(submission.getSubmissionDate())
                .submissionDocument(submission.getSubmissionDocument())
                .matchedKeywords(submission.getMatchedKeywords())
                .missingKeywords(submission.getMissingKeywords())
                .percentage(submission.getPercentage())
                .evaluationDate(submission.getEvaluationDate())
                .build();
    }

    @Transactional
    public AssignmentSubmissionResponse submitAssignmentFile(Long assignmentId, Long studentUserId, String originalFilename, byte[] fileBytes) {
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment", assignmentId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndStudentId(assignmentId, student.getId())
                .orElse(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .student(student)
                        .build());

        String content = textExtractionService.extractText(originalFilename, fileBytes);
        submission.setSubmissionDocument(content);
        submission.setAnswer("Uploaded File: " + originalFilename);
        submission.setSubmissionDate(LocalDateTime.now());
        
        autoEvaluateAssignmentSubmission(submission);

        submission = submissionRepository.save(submission);

        if (assignment.getTrainer() != null) {
            notificationService.createNotification(assignment.getTrainer().getUser(), "Assignment Submitted",
                    student.getUser().getName() + " submitted assignment '" + assignment.getTitle() + "' via file upload.", "ASSIGNMENT");
        }
        
        notificationService.createNotification(student.getUser(), "Assignment Graded",
                "Your submission for assignment '" + assignment.getTitle() + "' has been automatically evaluated. Score: " + submission.getMarks() + "/" + assignment.getTotalMarks() + " (" + String.format("%.1f", submission.getPercentage()) + "% matched).", "ASSIGNMENT");

        return mapToSubmissionResponse(submission);
    }

    private void autoEvaluateAssignmentSubmission(AssignmentSubmission submission) {
        Assignment assignment = submission.getAssignment();
        if (assignment == null) return;
        
        String reqKeywordsStr = assignment.getRequiredKeywords();
        String studentDoc = submission.getSubmissionDocument();
        int totalMarks = assignment.getTotalMarks() != null ? assignment.getTotalMarks() : 100;
        
        com.skillsphere.common.service.KeywordEvaluationService.KeywordEvaluationResult result = 
                keywordEvaluationService.evaluate(studentDoc, reqKeywordsStr, totalMarks);
                
        submission.setMarks(result.getObtainedMarks());
        submission.setPercentage(result.getPercentage());
        submission.setMatchedKeywords(result.getMatchedList());
        submission.setMissingKeywords(result.getMissingList());
        submission.setEvaluationDate(LocalDateTime.now());
        submission.setStatus("GRADED");
    }
}
