package com.skillsphere.batch.service;

import com.skillsphere.batch.dto.BatchRequest;
import com.skillsphere.batch.dto.BatchResponse;
import com.skillsphere.batch.dto.EnrollmentResponse;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.entity.Enrollment;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.common.enums.BatchStatus;
import com.skillsphere.common.enums.EnrollmentStatus;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.dto.StudentResponse;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BatchService {

    private final BatchRepository batchRepository;
    private final CourseRepository courseRepository;
    private final TrainerRepository trainerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;
    private final com.skillsphere.payment.repository.PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<BatchResponse> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatchById(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
        return mapToResponse(batch);
    }

    @Transactional
    public BatchResponse createBatch(BatchRequest request) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        Trainer trainer = null;
        if (request.getTrainerId() != null) {
            trainer = trainerRepository.findById(request.getTrainerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trainer", request.getTrainerId()));
        }

        Batch batch = Batch.builder()
                .batchName(request.getBatchName())
                .course(course)
                .trainer(trainer)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxStudents(request.getMaxStudents())
                .status(request.getStatus() != null ? BatchStatus.valueOf(request.getStatus()) : BatchStatus.UPCOMING)
                .meetingLink(request.getMeetingLink())
                .meetingTitle(request.getMeetingTitle())
                .build();

        batch = batchRepository.save(batch);
        return mapToResponse(batch);
    }

    @Transactional
    public BatchResponse updateBatch(Long id, BatchRequest request) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        Trainer trainer = null;
        if (request.getTrainerId() != null) {
            trainer = trainerRepository.findById(request.getTrainerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Trainer", request.getTrainerId()));
        }

        batch.setBatchName(request.getBatchName());
        batch.setCourse(course);
        batch.setTrainer(trainer);
        batch.setStartDate(request.getStartDate());
        batch.setEndDate(request.getEndDate());
        batch.setMaxStudents(request.getMaxStudents());
        batch.setMeetingLink(request.getMeetingLink());
        batch.setMeetingTitle(request.getMeetingTitle());
        if (request.getStatus() != null) {
            batch.setStatus(BatchStatus.valueOf(request.getStatus()));
        }

        batch = batchRepository.save(batch);
        return mapToResponse(batch);
    }

    @Transactional
    public void deleteBatch(Long id) {
        Batch batch = batchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", id));
        batchRepository.delete(batch);
    }

    @Transactional
    public EnrollmentResponse enrollStudent(Long batchId, Long studentId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", batchId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        if (enrollmentRepository.existsByStudentIdAndBatchId(studentId, batchId)) {
            throw new DuplicateResourceException("Student is already enrolled in this batch");
        }

        long activeEnrollments = enrollmentRepository.countActiveEnrollmentsByBatchId(batchId);
        if (activeEnrollments >= batch.getMaxStudents()) {
            throw new BadRequestException("Batch capacity has reached its limit (" + batch.getMaxStudents() + ")");
        }

        Optional<Enrollment> pendingOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, batch.getCourse().getId());
        if (pendingOpt.isEmpty()) {
            throw new BadRequestException("Student is not enrolled in the course: " + batch.getCourse().getName());
        }
        Enrollment enrollment = pendingOpt.get();
        enrollment.setBatch(batch);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setEnrollmentDate(LocalDate.now());

        enrollment = enrollmentRepository.save(enrollment);

        // Notify student
        notificationService.createNotification(student.getUser(), "Enrolled in Batch",
                "You have been successfully enrolled in batch " + batch.getBatchName() + " for course " + batch.getCourse().getName(), "BATCH");

        return mapToEnrollmentResponse(enrollment);
    }

    @Transactional
    public void removeStudent(Long batchId, Long studentId) {
        Enrollment enrollment = enrollmentRepository.findByStudentIdAndBatchId(studentId, batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found for student " + studentId + " and batch " + batchId));
        enrollment.setBatch(null);
        enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getEnrolledStudents(Long batchId) {
        List<Enrollment> enrollments = enrollmentRepository.findByBatchId(batchId);
        return enrollments.stream()
                .map(e -> mapToStudentResponse(e.getStudent()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getStudentBatches(Long studentId) {
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        return enrollments.stream()
                .filter(e -> e.getBatch() != null)
                .map(e -> mapToResponse(e.getBatch()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getTrainerBatches(Long trainerId) {
        List<Batch> batches = batchRepository.findByTrainerId(trainerId);
        return batches.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getActiveBatchesByCourseId(Long courseId) {
        return batchRepository.findByCourseIdAndStatus(courseId, com.skillsphere.common.enums.BatchStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getEligibleStudentsForCourse(Long courseId) {
        return enrollmentRepository.findAll().stream()
                .filter(e -> e.getCourse() != null && e.getCourse().getId().equals(courseId) && e.getStatus() == com.skillsphere.common.enums.EnrollmentStatus.ACTIVE)
                .map(e -> mapToStudentResponse(e.getStudent()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollments(Long courseId) {
        List<Enrollment> list;
        if (courseId != null) {
            list = enrollmentRepository.findAll().stream()
                    .filter(e -> e.getCourse() != null && e.getCourse().getId().equals(courseId))
                    .collect(Collectors.toList());
        } else {
            list = enrollmentRepository.findAll();
        }
        return list.stream()
                .map(this::mapToEnrollmentResponse)
                .collect(Collectors.toList());
    }

    private BatchResponse mapToResponse(Batch batch) {
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

    public EnrollmentResponse mapToEnrollmentResponse(Enrollment enrollment) {
        String cName = "Unknown Course";
        Long courseId = null;
        if (enrollment.getCourse() != null) {
            cName = enrollment.getCourse().getName();
            courseId = enrollment.getCourse().getId();
        } else if (enrollment.getBatch() != null && enrollment.getBatch().getCourse() != null) {
            cName = enrollment.getBatch().getCourse().getName();
            courseId = enrollment.getBatch().getCourse().getId();
        }

        String trainerName = "UNASSIGNED";
        LocalDate startDate = null;
        LocalDate endDate = null;
        String meetingLink = null;
        String meetingTitle = null;

        if (enrollment.getBatch() != null) {
            Batch batch = enrollment.getBatch();
            if (batch.getTrainer() != null && batch.getTrainer().getUser() != null) {
                trainerName = batch.getTrainer().getUser().getName();
            }
            startDate = batch.getStartDate();
            endDate = batch.getEndDate();
            meetingLink = batch.getMeetingLink();
            meetingTitle = batch.getMeetingTitle();
        }

        // Determine payment status (e.g. PAID, PARTIALLY_PAID, PENDING)
        String paymentStatus = "PENDING";
        if (courseId != null) {
            List<com.skillsphere.payment.entity.Payment> payments = paymentRepository.findByStudentIdAndCourseId(enrollment.getStudent().getId(), courseId);
            if (!payments.isEmpty()) {
                boolean hasPaid = payments.stream().anyMatch(p -> p.getStatus() == com.skillsphere.common.enums.PaymentStatus.PAID);
                boolean hasPartial = payments.stream().anyMatch(p -> p.getStatus() == com.skillsphere.common.enums.PaymentStatus.PARTIALLY_PAID);
                if (hasPaid) {
                    java.math.BigDecimal remaining = payments.stream()
                        .filter(p -> p.getStatus() == com.skillsphere.common.enums.PaymentStatus.PAID)
                        .map(com.skillsphere.payment.entity.Payment::getRemainingAmount)
                        .reduce((first, second) -> second)
                        .orElse(java.math.BigDecimal.ZERO);
                    if (remaining.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        paymentStatus = "PAID";
                    } else {
                        paymentStatus = "PARTIALLY_PAID";
                    }
                } else if (hasPartial) {
                    paymentStatus = "PARTIALLY_PAID";
                }
            }
        }

        String studentName = "Student";
        String regNum = "-";
        Long studentId = null;
        if (enrollment.getStudent() != null) {
            studentId = enrollment.getStudent().getId();
            regNum = enrollment.getStudent().getRegistrationNumber();
            if (enrollment.getStudent().getUser() != null) {
                studentName = enrollment.getStudent().getUser().getName();
            }
        }

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(studentId)
                .studentName(studentName)
                .registrationNumber(regNum)
                .batchId(enrollment.getBatch() != null ? enrollment.getBatch().getId() : null)
                .batchName(enrollment.getBatch() != null ? enrollment.getBatch().getBatchName() : "Not Assigned")
                .courseId(courseId)
                .courseName(cName)
                .enrollmentDate(enrollment.getEnrollmentDate())
                .status(enrollment.getStatus() != null ? enrollment.getStatus().name() : "PENDING")
                .trainerName(trainerName)
                .startDate(startDate)
                .endDate(endDate)
                .meetingLink(meetingLink)
                .meetingTitle(meetingTitle)
                .paymentStatus(paymentStatus)
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
}
