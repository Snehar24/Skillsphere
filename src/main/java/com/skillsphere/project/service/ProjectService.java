package com.skillsphere.project.service;

import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.project.dto.*;
import com.skillsphere.project.entity.Project;
import com.skillsphere.project.entity.ProjectSubmission;
import com.skillsphere.project.repository.ProjectRepository;
import com.skillsphere.project.repository.ProjectSubmissionRepository;
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
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectSubmissionRepository submissionRepository;
    private final BatchRepository batchRepository;
    private final TrainerRepository trainerRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final com.skillsphere.common.service.DocumentTextExtractionService textExtractionService;
    private final com.skillsphere.common.service.KeywordEvaluationService keywordEvaluationService;

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(ProjectRequest request, Long trainerUserId) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        Trainer trainer = trainerRepository.findByUserId(trainerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer not found for user: " + trainerUserId));

        Project project = Project.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .batch(batch)
                .trainer(trainer)
                .deadline(request.getDeadline())
                .totalMarks(request.getTotalMarks())
                .trainerContent(request.getTrainerContent())
                .requiredKeywords(request.getRequiredKeywords())
                .status("ACTIVE")
                .build();

        project = projectRepository.save(project);

        // Notify enrolled students
        enrollmentRepository.findByBatchId(batch.getId()).forEach(enrollment -> {
            notificationService.createNotification(enrollment.getStudent().getUser(), "New Project Released",
                    "New project '" + request.getTitle() + "' has been assigned. Deadline: " + request.getDeadline(), "PROJECT");
        });

        return mapToResponse(project);
    }

    @Transactional
    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        project.setTitle(request.getTitle());
        project.setDescription(request.getDescription());
        project.setDeadline(request.getDeadline());
        project.setTotalMarks(request.getTotalMarks());
        project.setTrainerContent(request.getTrainerContent());
        project.setRequiredKeywords(request.getRequiredKeywords());
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }

        project = projectRepository.save(project);
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        projectRepository.delete(project);
    }

    @Transactional
    public ProjectSubmissionResponse submitProject(Long projectId, Long studentUserId, ProjectSubmissionRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        // Create or update project submission
        List<ProjectSubmission> existing = submissionRepository.findByStudentId(student.getId());
        ProjectSubmission submission = existing.stream()
                .filter(ps -> ps.getProject().getId().equals(projectId))
                .findFirst()
                .orElse(ProjectSubmission.builder().project(project).student(student).build());

        submission.setRepositoryUrl(request.getRepositoryUrl());
        submission.setDocumentation(request.getDocumentation());
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);

        if (project.getTrainer() != null) {
            notificationService.createNotification(project.getTrainer().getUser(), "Project Submitted",
                    student.getUser().getName() + " submitted project '" + project.getTitle() + "'", "PROJECT");
        }

        return mapToSubmissionResponse(submission);
    }

    @Transactional
    public ProjectSubmissionResponse gradeProjectSubmission(Long submissionId, ProjectEvaluationRequest request) {
        ProjectSubmission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", submissionId));

        if (request.getMarks() > submission.getProject().getTotalMarks()) {
            throw new BadRequestException("Marks cannot exceed total project marks (" + submission.getProject().getTotalMarks() + ")");
        }

        submission.setMarks(request.getMarks());
        submission.setFeedback(request.getFeedback());
        submission.setStatus("GRADED");

        submission = submissionRepository.save(submission);

        // Notify student
        notificationService.createNotification(submission.getStudent().getUser(), "Project Graded",
                "Your submission for '" + submission.getProject().getTitle() + "' has been graded. Marks: " + request.getMarks(), "PROJECT");

        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public ProjectSubmissionResponse getSubmissionById(Long id) {
        ProjectSubmission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Submission", id));
        return mapToSubmissionResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<ProjectSubmissionResponse> getProjectSubmissions(Long projectId) {
        return submissionRepository.findByProjectId(projectId).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectSubmissionResponse> getStudentProjectSubmissions(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return submissionRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToSubmissionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getBatchProjects(Long batchId) {
        return projectRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ProjectResponse mapToResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .batchId(project.getBatch() != null ? project.getBatch().getId() : null)
                .batchName(project.getBatch() != null ? project.getBatch().getBatchName() : null)
                .trainerId(project.getTrainer() != null ? project.getTrainer().getId() : null)
                .trainerName(project.getTrainer() != null ? project.getTrainer().getUser().getName() : "UNASSIGNED")
                .deadline(project.getDeadline())
                .totalMarks(project.getTotalMarks())
                .trainerContent(project.getTrainerContent())
                .requiredKeywords(project.getRequiredKeywords())
                .status(project.getStatus())
                .build();
    }

    private ProjectSubmissionResponse mapToSubmissionResponse(ProjectSubmission submission) {
        return ProjectSubmissionResponse.builder()
                .id(submission.getId())
                .projectId(submission.getProject().getId())
                .projectTitle(submission.getProject().getTitle())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getUser().getName())
                .repositoryUrl(submission.getRepositoryUrl())
                .documentation(submission.getDocumentation())
                .marks(submission.getMarks())
                .feedback(submission.getFeedback())
                .status(submission.getStatus())
                .submittedAt(submission.getSubmittedAt())
                .submissionDocument(submission.getSubmissionDocument())
                .matchedKeywords(submission.getMatchedKeywords())
                .missingKeywords(submission.getMissingKeywords())
                .percentage(submission.getPercentage())
                .evaluationDate(submission.getEvaluationDate())
                .build();
    }

    @Transactional
    public ProjectSubmissionResponse submitProjectFile(Long projectId, Long studentUserId, String originalFilename, byte[] fileBytes) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        ProjectSubmission submission = submissionRepository.findByProjectIdAndStudentId(projectId, student.getId())
                .orElse(ProjectSubmission.builder()
                        .project(project)
                        .student(student)
                        .build());

        String content = textExtractionService.extractText(originalFilename, fileBytes);
        submission.setSubmissionDocument(content);
        submission.setDocumentation("Uploaded File: " + originalFilename);
        submission.setRepositoryUrl("Uploaded File: " + originalFilename);
        submission.setSubmittedAt(LocalDateTime.now());
        
        autoEvaluateProjectSubmission(submission);

        submission = submissionRepository.save(submission);

        if (project.getTrainer() != null) {
            notificationService.createNotification(project.getTrainer().getUser(), "Project Submitted",
                    student.getUser().getName() + " submitted project '" + project.getTitle() + "' via file upload.", "PROJECT");
        }
        
        notificationService.createNotification(student.getUser(), "Project Graded",
                "Your submission for project '" + project.getTitle() + "' has been automatically evaluated. Score: " + submission.getMarks() + "/" + project.getTotalMarks() + " (" + String.format("%.1f", submission.getPercentage()) + "% matched).", "PROJECT");

        return mapToSubmissionResponse(submission);
    }

    private void autoEvaluateProjectSubmission(ProjectSubmission submission) {
        Project project = submission.getProject();
        if (project == null) return;
        
        String reqKeywordsStr = project.getRequiredKeywords();
        String studentDoc = submission.getSubmissionDocument();
        int totalMarks = project.getTotalMarks() != null ? project.getTotalMarks() : 100;
        
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
