package com.skillsphere.exam.service;

import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.entity.Enrollment;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import java.util.Optional;
import com.skillsphere.exam.dto.*;
import com.skillsphere.exam.entity.Exam;
import com.skillsphere.exam.entity.ExamResult;
import com.skillsphere.exam.entity.Question;
import com.skillsphere.exam.repository.ExamRepository;
import com.skillsphere.exam.repository.ExamResultRepository;
import com.skillsphere.exam.repository.QuestionRepository;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.skillsphere.exam.entity.ExamAttempt;
import com.skillsphere.exam.repository.ExamAttemptRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final ExamResultRepository examResultRepository;
    private final BatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final ExamAttemptRepository examAttemptRepository;
    private final com.skillsphere.common.service.AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<ExamResponse> getAllExams() {
        return examRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getBatchExams(Long batchId) {
        return examRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExamResponse getExamById(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        return mapToResponse(exam);
    }

    @Transactional
    public ExamResponse createExam(ExamRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        Exam exam = Exam.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .course(batch.getCourse())
                .batch(batch)
                .duration(request.getDuration())
                .totalMarks(request.getTotalMarks())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status("UPCOMING")
                .build();

        exam = examRepository.save(exam);

        // Notify enrolled students
        enrollmentRepository.findByBatchId(batch.getId()).forEach(enrollment -> {
            notificationService.createNotification(enrollment.getStudent().getUser(), "New Exam Scheduled",
                    "New exam '" + request.getTitle() + "' is scheduled.", "EXAM");
        });

        return mapToResponse(exam);
    }

    @Transactional
    public ExamResponse updateExam(Long id, ExamRequest request) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", id));

        exam.setTitle(request.getTitle());
        exam.setDescription(request.getDescription());
        exam.setDuration(request.getDuration());
        exam.setTotalMarks(request.getTotalMarks());
        exam.setStartTime(request.getStartTime());
        exam.setEndTime(request.getEndTime());
        if (request.getStatus() != null) {
            exam.setStatus(request.getStatus());
        }

        exam = examRepository.save(exam);
        return mapToResponse(exam);
    }

    @Transactional
    public void deleteExam(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", id));
        examRepository.delete(exam);
    }

    @Transactional
    public QuestionResponse addQuestion(Long examId, QuestionRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        Question question = Question.builder()
                .exam(exam)
                .questionText(request.getQuestionText())
                .optionA(request.getOptionA())
                .optionB(request.getOptionB())
                .optionC(request.getOptionC())
                .optionD(request.getOptionD())
                .correctAnswer(request.getCorrectAnswer().toUpperCase())
                .marks(request.getMarks())
                .build();

        question = questionRepository.save(question);
        return mapToQuestionResponse(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        questionRepository.delete(question);
    }

    @Transactional
    public QuestionResponse updateQuestion(Long questionId, QuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        question.setQuestionText(request.getQuestionText());
        question.setOptionA(request.getOptionA());
        question.setOptionB(request.getOptionB());
        question.setOptionC(request.getOptionC());
        question.setOptionD(request.getOptionD());
        question.setCorrectAnswer(request.getCorrectAnswer().toUpperCase().trim());
        question.setMarks(request.getMarks());
        question = questionRepository.save(question);
        return mapToQuestionResponse(question);
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getExamQuestions(Long examId, boolean hideCorrectAnswer) {
        List<Question> questions = questionRepository.findByExamId(examId);
        return questions.stream()
                .map(q -> {
                    QuestionResponse res = mapToQuestionResponse(q);
                    if (hideCorrectAnswer) {
                        res.setCorrectAnswer(null); // Clear correct answer for student view
                    }
                    return res;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> getExamQuestionsForStudent(Long examId, Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId).orElse(null);
        boolean hasSubmitted = false;
        if (student != null) {
            hasSubmitted = examResultRepository.findByExamIdAndStudentId(examId, student.getId()).isPresent();
        }
        return getExamQuestions(examId, !hasSubmitted);
    }

    @Transactional
    public ExamResultResponse submitExam(Long examId, Long studentUserId, ExamSubmitRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));

        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        if (examResultRepository.findByExamIdAndStudentId(examId, student.getId()).isPresent()) {
            throw new BadRequestException("You have already submitted this exam");
        }

        List<Question> questions = questionRepository.findByExamId(examId);
        int score = 0;
        int totalPossibleMarks = 0;

        Map<Long, String> studentAnswers = request.getAnswers();

        for (Question q : questions) {
            totalPossibleMarks += q.getMarks();
            String submittedAnswer = studentAnswers != null ? studentAnswers.get(q.getId()) : null;
            if (isAnswerCorrect(submittedAnswer, q.getCorrectAnswer())) {
                score += q.getMarks();
            }
        }

        double percentage = 0.0;
        if (totalPossibleMarks > 0) {
            percentage = ((double) score / totalPossibleMarks) * 100.0;
        }

        String result = (percentage >= 40.0) ? "PASS" : "FAIL";

        ExamResult examResult = ExamResult.builder()
                .exam(exam)
                .student(student)
                .score(score)
                .percentage(percentage)
                .result(result)
                .submittedAt(LocalDateTime.now())
                .build();

        examResult = examResultRepository.save(examResult);

        // Notify student of result
        notificationService.createNotification(student.getUser(), "Exam Evaluated",
                "Your result for '" + exam.getTitle() + "' is " + result + ". Score: " + score + "/" + totalPossibleMarks, "EXAM");

        return mapToResultResponse(examResult);
    }

    @Transactional(readOnly = true)
    public ExamResultResponse getExamResult(Long examId, Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        ExamResult result = examResultRepository.findByExamIdAndStudentId(examId, student.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Result not found for exam: " + examId + " and student: " + student.getId()));
        return mapToResultResponse(result);
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> getStudentResults(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return examResultRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToResultResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamResultResponse> getExamSubmissions(Long examId) {
        return examResultRepository.findByExamId(examId).stream()
                .map(this::mapToResultResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getStudentExamsForDashboard(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());
        List<ExamResponse> exams = new java.util.ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            List<Exam> batchExams = examRepository.findByBatchId(enrollment.getBatch().getId());
            for (Exam e : batchExams) {
                long attemptsUsed = examAttemptRepository.countByExamIdAndStudentId(e.getId(), student.getId());
                long allowed = e.getAllowedAttempts() != null ? e.getAllowedAttempts() : 1;
                long attemptsRemaining = Math.max(0, allowed - attemptsUsed);
                int qCount = questionRepository.findByExamId(e.getId()).size();
                
                exams.add(ExamResponse.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .description(e.getDescription())
                        .courseId(e.getCourse() != null ? e.getCourse().getId() : null)
                        .courseName(e.getCourse() != null ? e.getCourse().getName() : null)
                        .batchId(e.getBatch() != null ? e.getBatch().getId() : null)
                        .batchName(e.getBatch() != null ? e.getBatch().getBatchName() : null)
                        .duration(e.getDuration())
                        .totalMarks(e.getTotalMarks())
                        .startTime(e.getStartTime())
                        .endTime(e.getEndTime())
                        .status(e.getStatus())
                        .questionCount(qCount)
                        .allowedAttempts(e.getAllowedAttempts() != null ? e.getAllowedAttempts() : 1)
                        .attemptsUsed(attemptsUsed)
                        .attemptsRemaining(attemptsRemaining)
                        .build());
            }
        }
        return exams;
    }

    @Transactional
    public ExamAttemptResponse startExamAttempt(Long examId, Long studentUserId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        boolean enrolled = enrollmentRepository.existsByStudentIdAndBatchId(student.getId(), exam.getBatch().getId());
        if (!enrolled) {
            throw new BadRequestException("You are not enrolled in the batch for this exam");
        }

        long attemptsUsed = examAttemptRepository.countByExamIdAndStudentId(examId, student.getId());
        long allowed = exam.getAllowedAttempts() != null ? exam.getAllowedAttempts() : 1;

        Optional<ExamAttempt> activeOpt = examAttemptRepository
                .findFirstByExamIdAndStudentIdAndSubmittedFalseOrderByAttemptNumberDesc(examId, student.getId());
        
        ExamAttempt attempt;
        if (activeOpt.isPresent()) {
            attempt = activeOpt.get();
        } else {
            if (attemptsUsed >= allowed) {
                throw new BadRequestException("No more attempts available for this exam");
            }
            attempt = ExamAttempt.builder()
                    .exam(exam)
                    .student(student)
                    .attemptNumber((int)(attemptsUsed + 1))
                    .startTime(LocalDateTime.now())
                    .submitted(false)
                    .build();
            attempt = examAttemptRepository.save(attempt);
        }

        return ExamAttemptResponse.builder()
                .attemptId(attempt.getId())
                .attemptNumber(attempt.getAttemptNumber())
                .startTime(attempt.getStartTime())
                .duration(exam.getDuration())
                .allowedAttempts((int)allowed)
                .attemptsUsed((int)(activeOpt.isPresent() ? attemptsUsed : attemptsUsed + 1))
                .build();
    }

    @Transactional
    public Map<String, Object> recordViolation(Long examId, Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        ExamAttempt attempt = examAttemptRepository
                .findFirstByExamIdAndStudentIdAndSubmittedFalseOrderByAttemptNumberDesc(examId, student.getId())
                .orElseThrow(() -> new BadRequestException("No active attempt found for this exam"));

        int count = attempt.getViolationCount() + 1;
        attempt.setViolationCount(count);
        examAttemptRepository.save(attempt);

        boolean autoSubmitted = false;
        if (count >= 3) {
            // Auto submit due to 3 fullscreen/tab violations
            ExamSubmitRequest autoReq = new ExamSubmitRequest();
            autoReq.setSubmissionType("FULLSCREEN_VIOLATION");
            autoReq.setViolationCount(count);
            try {
                if (attempt.getAnswersJson() != null && !attempt.getAnswersJson().isEmpty()) {
                    autoReq.setAnswers(new com.fasterxml.jackson.databind.ObjectMapper().readValue(attempt.getAnswersJson(), new com.fasterxml.jackson.core.type.TypeReference<Map<Long, String>>(){}));
                }
            } catch(Exception ignored) {}
            submitExamAttempt(examId, studentUserId, autoReq);
            autoSubmitted = true;
        }

        return Map.of(
                "violationCount", count,
                "maxViolations", 3,
                "autoSubmitted", autoSubmitted,
                "message", autoSubmitted ? "Exam auto-submitted due to 3 security violations." : "Security warning " + count + "/3: Fullscreen exit / tab switch detected!"
        );
    }

    @Transactional
    public ExamResultResponse submitExamAttempt(Long examId, Long studentUserId, ExamSubmitRequest request) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam", examId));
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));

        ExamAttempt attempt = examAttemptRepository
                .findFirstByExamIdAndStudentIdAndSubmittedFalseOrderByAttemptNumberDesc(examId, student.getId())
                .orElseThrow(() -> new BadRequestException("No active attempt found for this exam"));

        List<Question> questions = questionRepository.findByExamId(examId);
        int score = 0;
        int totalPossibleMarks = 0;
        int correctAnswers = 0;
        int wrongAnswers = 0;

        Map<Long, String> studentAnswers = request != null ? request.getAnswers() : null;

        for (Question q : questions) {
            if (q.getCorrectAnswer() == null || q.getCorrectAnswer().trim().isEmpty()) {
                throw new BadRequestException("Exam evaluation failed because the correct answer is missing.");
            }
            totalPossibleMarks += q.getMarks();
            String submittedAnswer = studentAnswers != null ? studentAnswers.get(q.getId()) : null;
            if (isAnswerCorrect(submittedAnswer, q.getCorrectAnswer())) {
                score += q.getMarks();
                correctAnswers++;
            } else {
                wrongAnswers++;
            }
        }

        double percentage = 0.0;
        if (totalPossibleMarks > 0) {
            percentage = ((double) score / totalPossibleMarks) * 100.0;
        }

        String result = (percentage >= 40.0) ? "PASS" : "FAIL";
        String submissionType = (request != null && request.getSubmissionType() != null) ? request.getSubmissionType() : "MANUAL";
        int violations = (request != null && request.getViolationCount() != null) ? request.getViolationCount() : attempt.getViolationCount();

        attempt.setEndTime(LocalDateTime.now());
        attempt.setScore(score);
        attempt.setMaxMarks(totalPossibleMarks);
        attempt.setPercentage(percentage);
        attempt.setResult(result);
        attempt.setSubmitted(true);
        attempt.setSubmissionType(submissionType);
        attempt.setViolationCount(violations);
        try {
            attempt.setAnswersJson(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(studentAnswers != null ? studentAnswers : Map.of()));
        } catch (Exception e) {
            attempt.setAnswersJson("{}");
        }
        examAttemptRepository.save(attempt);

        Optional<ExamResult> resultOpt = examResultRepository.findByExamIdAndStudentId(examId, student.getId());
        ExamResult examResult;
        if (resultOpt.isPresent()) {
            examResult = resultOpt.get();
            if (score > examResult.getScore()) {
                examResult.setScore(score);
                examResult.setPercentage(percentage);
                examResult.setResult(result);
                examResult.setAttemptNumber(attempt.getAttemptNumber());
                examResult.setTotalQuestions(questions.size());
                examResult.setCorrectAnswers(correctAnswers);
                examResult.setWrongAnswers(wrongAnswers);
                examResult.setMaxMarks(totalPossibleMarks);
                examResult.setSubmissionType(submissionType);
                examResult.setViolationCount(violations);
                examResult.setSubmittedAt(LocalDateTime.now());
            }
        } else {
            examResult = ExamResult.builder()
                    .exam(exam)
                    .student(student)
                    .score(score)
                    .percentage(percentage)
                    .result(result)
                    .attemptNumber(attempt.getAttemptNumber())
                    .totalQuestions(questions.size())
                    .correctAnswers(correctAnswers)
                    .wrongAnswers(wrongAnswers)
                    .maxMarks(totalPossibleMarks)
                    .submissionType(submissionType)
                    .violationCount(violations)
                    .build();
        }
        examResult = examResultRepository.save(examResult);

        // Audit Log
        auditLogService.log("EXAM_SUBMIT", "Exam", examId.toString(),
                student.getUser().getId(), student.getUser().getEmail(), "ROLE_STUDENT",
                "Submitted exam: " + exam.getTitle() + " (Score: " + score + "/" + totalPossibleMarks + ", Type: " + submissionType + ", Violations: " + violations + ")", null);

        // Notify student
        notificationService.createNotification(student.getUser(), "Exam Attempt Evaluated",
                "Your attempt #" + attempt.getAttemptNumber() + " for '" + exam.getTitle() + "' is " + result + ". Score: " + score + "/" + totalPossibleMarks + " (" + submissionType + ")", "EXAM");

        return mapToResultResponse(examResult);
    }

    @Transactional
    public ExamResponse createExamFromDocument(Long batchId, String title, String description, Integer duration, Integer allowedAttempts, String documentText) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));

        Exam exam = Exam.builder()
                .title(title)
                .description(description)
                .course(batch.getCourse())
                .batch(batch)
                .duration(duration)
                .allowedAttempts(allowedAttempts)
                .totalMarks(0)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .status("ACTIVE")
                .build();
        exam = examRepository.save(exam);

        int totalMarks = 0;
        String[] blocks = documentText.split("(?i)Question\\s+\\d+[:\\.]?\\s*");
        for (String block : blocks) {
            if (block.trim().isEmpty()) continue;
            
            String[] lines = block.split("\\r?\\n");
            StringBuilder qTextBuilder = new StringBuilder();
            String optA = null, optB = null, optC = null, optD = null;
            String answer = null;
            
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                
                String upper = trimmed.toUpperCase();
                if (upper.startsWith("A.") || upper.startsWith("A)")) {
                    optA = trimmed.substring(2).trim();
                } else if (upper.startsWith("B.") || upper.startsWith("B)")) {
                    optB = trimmed.substring(2).trim();
                } else if (upper.startsWith("C.") || upper.startsWith("C)")) {
                    optC = trimmed.substring(2).trim();
                } else if (upper.startsWith("D.") || upper.startsWith("D)")) {
                    optD = trimmed.substring(2).trim();
                } else if (trimmed.toLowerCase().startsWith("answer:") || trimmed.toLowerCase().replaceAll("\\s+", "").startsWith("answer:")) {
                    int colonIdx = trimmed.indexOf(":");
                    if (colonIdx >= 0) {
                        String ansVal = trimmed.substring(colonIdx + 1).trim().toUpperCase();
                        if (ansVal.matches("^[A-D](\\..*|\\).*|\\s.*)?$")) {
                            answer = String.valueOf(ansVal.charAt(0));
                        } else {
                            answer = ansVal;
                        }
                    }
                } else {
                    if (qTextBuilder.length() > 0) qTextBuilder.append(" ");
                    qTextBuilder.append(trimmed);
                }
            }
            
            String qText = qTextBuilder.toString().trim();
            if (qText.isEmpty() || answer == null || answer.isEmpty()) continue;
            
            int questionMarks = 5;
            totalMarks += questionMarks;
            
            Question question = Question.builder()
                    .exam(exam)
                    .questionText(qText)
                    .optionA(optA)
                    .optionB(optB)
                    .optionC(optC)
                    .optionD(optD)
                    .correctAnswer(answer)
                    .marks(questionMarks)
                    .build();
            questionRepository.save(question);
        }

        exam.setTotalMarks(totalMarks);
        exam = examRepository.save(exam);
        
        // Notify enrolled students
        final Exam finalExam = exam;
        enrollmentRepository.findByBatchId(batch.getId()).forEach(enrollment -> {
            notificationService.createNotification(enrollment.getStudent().getUser(), "New Exam Released",
                    "New exam '" + finalExam.getTitle() + "' is available.", "EXAM");
        });
        
        return mapToResponse(exam);
    }

    private ExamResponse mapToResponse(Exam exam) {
        int qCount = questionRepository.findByExamId(exam.getId()).size();
        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .description(exam.getDescription())
                .courseId(exam.getCourse() != null ? exam.getCourse().getId() : null)
                .courseName(exam.getCourse() != null ? exam.getCourse().getName() : null)
                .batchId(exam.getBatch() != null ? exam.getBatch().getId() : null)
                .batchName(exam.getBatch() != null ? exam.getBatch().getBatchName() : null)
                .duration(exam.getDuration())
                .totalMarks(exam.getTotalMarks())
                .startTime(exam.getStartTime())
                .endTime(exam.getEndTime())
                .status(exam.getStatus())
                .questionCount(qCount)
                .allowedAttempts(exam.getAllowedAttempts() != null ? exam.getAllowedAttempts() : 1)
                .build();
    }

    private QuestionResponse mapToQuestionResponse(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .examId(question.getExam().getId())
                .questionText(question.getQuestionText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .correctAnswer(question.getCorrectAnswer())
                .marks(question.getMarks())
                .build();
    }

    private ExamResultResponse mapToResultResponse(ExamResult result) {
        return ExamResultResponse.builder()
                .id(result.getId())
                .examId(result.getExam().getId())
                .examTitle(result.getExam().getTitle())
                .studentId(result.getStudent().getId())
                .studentName(result.getStudent().getUser().getName())
                .registrationNumber(result.getStudent().getRegistrationNumber())
                .batchName(result.getExam().getBatch() != null ? result.getExam().getBatch().getBatchName() : null)
                .attemptNumber(result.getAttemptNumber())
                .totalQuestions(result.getTotalQuestions())
                .correctAnswers(result.getCorrectAnswers())
                .wrongAnswers(result.getWrongAnswers())
                .maxMarks(result.getMaxMarks())
                .score(result.getScore())
                .percentage(result.getPercentage())
                .result(result.getResult())
                .submissionType(result.getSubmissionType() != null ? result.getSubmissionType() : "MANUAL")
                .violationCount(result.getViolationCount())
                .submittedAt(result.getSubmittedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Long getExamIdByQuestionId(Long questionId) {
        Question q = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));
        return q.getExam().getId();
    }

    private boolean isAnswerCorrect(String studentAns, String correctAns) {
        if (studentAns == null || correctAns == null) return false;
        String s = studentAns.trim().toUpperCase();
        String c = correctAns.trim().toUpperCase();
        if (s.equals(c)) return true;
        if (c.length() > 0 && (c.charAt(0) >= 'A' && c.charAt(0) <= 'D')) {
            if (s.equals(String.valueOf(c.charAt(0)))) return true;
        }
        return false;
    }
}
