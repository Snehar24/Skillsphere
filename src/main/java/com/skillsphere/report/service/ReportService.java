package com.skillsphere.report.service;

import com.skillsphere.attendance.repository.AttendanceRepository;
import com.skillsphere.attendance.service.AttendanceService;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.entity.Enrollment;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.certificate.entity.Certificate;
import com.skillsphere.certificate.repository.CertificateRepository;
import com.skillsphere.common.enums.EnrollmentStatus;
import com.skillsphere.common.enums.Role;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.payment.entity.Payment;
import com.skillsphere.payment.repository.PaymentRepository;
import com.skillsphere.report.dto.ReportResponse;
import com.skillsphere.report.dto.StudentPerformanceReportResponse;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CertificateRepository certificateRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final AttendanceService attendanceService;
    private final StudentRepository studentRepository;
    private final com.skillsphere.exam.repository.ExamResultRepository examResultRepository;
    private final com.skillsphere.assignment.repository.AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final com.skillsphere.project.repository.ProjectSubmissionRepository projectSubmissionRepository;

    @Transactional(readOnly = true)
    public ReportResponse generateReport() {
        long students = userRepository.countByRole(Role.ROLE_STUDENT);
        long trainers = userRepository.countByRole(Role.ROLE_TRAINER);
        long courses = courseRepository.count();
        long batches = batchRepository.count();
        long enrollments = enrollmentRepository.count();
        long certificates = certificateRepository.count();

        BigDecimal revenueVal = paymentRepository.sumPaidPayments();
        BigDecimal revenue = revenueVal != null ? revenueVal : BigDecimal.ZERO;

        // Students per course
        Map<String, Long> studentsPerCourse = new HashMap<>();
        List<Course> allCourses = courseRepository.findAll();
        List<Enrollment> allEnrollments = enrollmentRepository.findAll();

        for (Course c : allCourses) {
            long count = allEnrollments.stream()
                    .filter(e -> (e.getCourse() != null && e.getCourse().getId().equals(c.getId())) ||
                            (e.getBatch() != null && e.getBatch().getCourse() != null && e.getBatch().getCourse().getId().equals(c.getId())))
                    .count();
            studentsPerCourse.put(c.getName(), count);
        }

        // Students per batch & attendance rates
        Map<String, Long> studentsPerBatch = new HashMap<>();
        List<Batch> allBatches = batchRepository.findAll();
        Map<String, Double> attendanceRatePerBatch = new HashMap<>();

        for (Batch b : allBatches) {
            long count = enrollmentRepository.countActiveEnrollmentsByBatchId(b.getId());
            studentsPerBatch.put(b.getBatchName(), count);

            List<Enrollment> enrolled = enrollmentRepository.findByBatchId(b.getId());
            double totalPercent = 0.0;
            int studentsWithAttendance = 0;
            for (Enrollment e : enrolled) {
                if (e.getStudent() != null) {
                    Map<String, Object> summary = attendanceService.getStudentAttendanceSummary(e.getStudent().getId(), b.getId());
                    Double percent = (summary.get("percentage") instanceof Number) ? ((Number) summary.get("percentage")).doubleValue() : null;
                    Long totalCls = (summary.get("totalClasses") instanceof Number) ? ((Number) summary.get("totalClasses")).longValue() : 0L;
                    if (percent != null && totalCls > 0) {
                        totalPercent += percent;
                        studentsWithAttendance++;
                    }
                }
            }
            double avgRate = studentsWithAttendance > 0 ? (totalPercent / studentsWithAttendance) : 0.0;
            attendanceRatePerBatch.put(b.getBatchName(), Math.round(avgRate * 100.0) / 100.0);
        }

        return ReportResponse.builder()
                .totalStudents(students)
                .totalTrainers(trainers)
                .totalCourses(courses)
                .totalBatches(batches)
                .totalEnrollments(enrollments)
                .totalCertificates(certificates)
                .totalRevenue(revenue)
                .studentsPerCourse(studentsPerCourse)
                .studentsPerBatch(studentsPerBatch)
                .attendanceRatePerBatch(attendanceRatePerBatch)
                .build();
    }

    @Transactional(readOnly = true)
    public List<StudentPerformanceReportResponse> getStudentPerformanceReports(Long filterCourseId, Long filterBatchId, String search) {
        List<Student> students = studentRepository.findAll();
        List<StudentPerformanceReportResponse> reports = new ArrayList<>();

        for (Student student : students) {
            if (student.getUser() == null) continue;

            String sName = student.getUser().getName();
            String sEmail = student.getUser().getEmail();
            String sReg = student.getRegistrationNumber();

            if (search != null && !search.trim().isEmpty()) {
                String q = search.trim().toLowerCase();
                boolean match = (sName != null && sName.toLowerCase().contains(q)) ||
                                (sEmail != null && sEmail.toLowerCase().contains(q)) ||
                                (sReg != null && sReg.toLowerCase().contains(q));
                if (!match) continue;
            }

            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

            if (enrollments.isEmpty()) {
                // Standalone student with no course enrollments yet
                if (filterCourseId == null && filterBatchId == null) {
                    reports.add(buildReportRow(student, null, null, null));
                }
            } else {
                for (Enrollment enr : enrollments) {
                    Course course = enr.getCourse() != null ? enr.getCourse() : (enr.getBatch() != null ? enr.getBatch().getCourse() : null);
                    Batch batch = enr.getBatch();

                    if (filterCourseId != null && (course == null || !course.getId().equals(filterCourseId))) {
                        continue;
                    }
                    if (filterBatchId != null && (batch == null || !batch.getId().equals(filterBatchId))) {
                        continue;
                    }

                    reports.add(buildReportRow(student, enr, course, batch));
                }
            }
        }

        return reports;
    }

    private StudentPerformanceReportResponse buildReportRow(Student student, Enrollment enrollment, Course course, Batch batch) {
        Long studentId = student.getId();
        Long courseId = course != null ? course.getId() : null;
        String courseName = course != null ? course.getName() : "None";
        Long batchId = batch != null ? batch.getId() : null;
        String batchName = batch != null ? batch.getBatchName() : "Not Assigned";

        // Attendance
        Map<String, Object> attSummary = attendanceService.getStudentAttendanceSummary(studentId, batchId);
        long totalClasses = (attSummary.get("totalClasses") instanceof Number) ? ((Number) attSummary.get("totalClasses")).longValue() : 0;
        long present = (attSummary.get("present") instanceof Number) ? ((Number) attSummary.get("present")).longValue() : 0;
        long late = (attSummary.get("late") instanceof Number) ? ((Number) attSummary.get("late")).longValue() : 0;
        double attPct = (attSummary.get("percentage") instanceof Number) ? ((Number) attSummary.get("percentage")).doubleValue() : 0.0;

        // Exams
        List<com.skillsphere.exam.entity.ExamResult> examResults = examResultRepository.findByStudentId(studentId);
        if (batchId != null) {
            examResults = examResults.stream()
                    .filter(er -> er.getExam() != null && er.getExam().getBatch() != null && er.getExam().getBatch().getId().equals(batchId))
                    .collect(Collectors.toList());
        }
        int examCount = examResults.size();
        double avgExam = examResults.isEmpty() ? 0.0 : examResults.stream()
                .mapToDouble(er -> er.getPercentage() != null ? er.getPercentage() : 0.0)
                .average().orElse(0.0);
        String examStatus = examCount == 0 ? "No Exams" : (avgExam >= 50.0 ? "PASSED (" + Math.round(avgExam) + "%)" : "FAILED (" + Math.round(avgExam) + "%)");

        // Assignments
        List<com.skillsphere.assignment.entity.AssignmentSubmission> assigns = assignmentSubmissionRepository.findByStudentId(studentId);
        int assignCount = assigns.size();
        double avgAssign = assigns.isEmpty() ? 0.0 : assigns.stream()
                .filter(a -> a.getMarks() != null)
                .mapToDouble(a -> a.getMarks().doubleValue())
                .average().orElse(0.0);

        // Projects
        List<com.skillsphere.project.entity.ProjectSubmission> projs = projectSubmissionRepository.findByStudentId(studentId);
        int projCount = projs.size();
        String projStatus = projCount == 0 ? "Pending" : "Submitted (" + projCount + ")";

        // Payment
        List<Payment> payments = paymentRepository.findByStudentId(studentId);
        String payStatus = "UNPAID";
        BigDecimal paidSum = BigDecimal.ZERO;
        BigDecimal totalFee = course != null && course.getFee() != null ? course.getFee() : BigDecimal.ZERO;

        for (Payment p : payments) {
            if (p.getAmountPaid() != null) {
                paidSum = paidSum.add(p.getAmountPaid());
            }
            if (courseId != null && p.getCourse() != null && p.getCourse().getId().equals(courseId)) {
                payStatus = p.getStatus() != null ? p.getStatus().name() : "PAID";
            }
        }
        if (paidSum.compareTo(BigDecimal.ZERO) > 0 && "UNPAID".equals(payStatus)) {
            payStatus = "PAID";
        }

        // Certificate
        Optional<Certificate> certOpt = (courseId != null) ?
                certificateRepository.findByStudentIdAndCourseId(studentId, courseId) : Optional.empty();
        boolean certIssued = certOpt.isPresent();
        String certNum = certIssued ? certOpt.get().getCertificateNumber() : null;
        String verifyCode = certIssued ? certOpt.get().getVerificationCode() : null;
        String grade = certIssued ? certOpt.get().getGrade() : null;
        java.time.LocalDate issueDate = certIssued ? certOpt.get().getIssueDate() : null;

        // Overall Score & Grade
        double overall = (avgExam * 0.4) + (attPct * 0.3) + (avgAssign * 0.15) + (projCount > 0 ? 15.0 : 0.0);
        if (overall == 0.0 && attPct > 0) overall = attPct;
        if (grade == null) {
            grade = (overall >= 90.0) ? "A+ Distinction" :
                    (overall >= 80.0) ? "A Excellent" :
                    (overall >= 70.0) ? "B Very Good" :
                    (overall >= 60.0) ? "C Good" :
                    (overall >= 50.0) ? "Pass" : "In Progress";
        }

        return StudentPerformanceReportResponse.builder()
                .studentId(studentId)
                .userId(student.getUser().getId())
                .studentName(student.getUser().getName())
                .email(student.getUser().getEmail())
                .phone(student.getUser().getPhone())
                .registrationNumber(student.getRegistrationNumber())
                .courseId(courseId)
                .courseName(courseName)
                .batchId(batchId)
                .batchName(batchName)
                .attendancePercentage(Math.round(attPct * 100.0) / 100.0)
                .totalClasses(totalClasses)
                .presentClasses(present + late)
                .examCount(examCount)
                .averageExamScore(Math.round(avgExam * 100.0) / 100.0)
                .examStatus(examStatus)
                .assignmentsSubmitted(assignCount)
                .averageAssignmentScore(Math.round(avgAssign * 100.0) / 100.0)
                .projectsSubmitted(projCount)
                .projectStatus(projStatus)
                .overallScore(Math.round(overall * 100.0) / 100.0)
                .grade(grade)
                .paymentStatus(payStatus)
                .amountPaid(paidSum)
                .totalCourseFee(totalFee)
                .certificateIssued(certIssued)
                .certificateNumber(certNum)
                .verificationCode(verifyCode)
                .certificateIssueDate(issueDate)
                .build();
    }
}
