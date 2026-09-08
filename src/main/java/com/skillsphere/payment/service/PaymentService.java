package com.skillsphere.payment.service;

import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.common.enums.PaymentStatus;
import com.skillsphere.common.service.AuditLogService;
import com.skillsphere.common.service.EmailService;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.payment.dto.PaymentRequest;
import com.skillsphere.payment.dto.PaymentResponse;
import com.skillsphere.payment.entity.Payment;
import com.skillsphere.payment.repository.PaymentRepository;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    private final EnrollmentRepository enrollmentRepository;
    private final com.skillsphere.notification.repository.NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    @Transactional
    public PaymentResponse recordPayment(PaymentRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.getStudentId()));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", request.getCourseId()));

        String transactionId = request.getTransactionId();
        if (transactionId == null || transactionId.trim().isEmpty()) {
            transactionId = "TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        }

        String receiptNumber = generateReceiptNumber();

        com.skillsphere.batch.entity.Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId()).orElse(null);

        BigDecimal totalCourseFee = course.getFee() != null ? course.getFee() : BigDecimal.ZERO;
        
        // Sum previous payments
        BigDecimal amountPaidSoFar = paymentRepository.findByStudentId(student.getId()).stream()
                .filter(p -> p.getCourse().getId().equals(course.getId()) && (p.getStatus() == PaymentStatus.PAID || p.getStatus() == PaymentStatus.PARTIALLY_PAID))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal totalPaid = amountPaidSoFar.add(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO);
        BigDecimal remainingAmount = totalCourseFee.subtract(totalPaid);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            remainingAmount = BigDecimal.ZERO;
        }

        PaymentStatus paymentStatus = request.getStatus() != null ? PaymentStatus.valueOf(request.getStatus().toUpperCase()) : 
                (remainingAmount.compareTo(BigDecimal.ZERO) <= 0 ? PaymentStatus.PAID : PaymentStatus.PARTIALLY_PAID);

        int instNum = request.getInstallmentNumber() != null ? request.getInstallmentNumber() : 
                ((int) paymentRepository.findByStudentId(student.getId()).stream()
                        .filter(p -> p.getCourse().getId().equals(course.getId()))
                        .count() + 1);

        LocalDate dueDate = request.getDueDate();
        if (dueDate == null && "EMI".equalsIgnoreCase(request.getPaymentMethod() != null ? request.getPaymentMethod() : "FULL")) {
            dueDate = LocalDate.now().plusMonths(instNum - 1);
        }

        Payment payment = Payment.builder()
                .student(student)
                .course(course)
                .enrollment(enrollment)
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .totalCourseFee(totalCourseFee)
                .amountPaid(totalPaid)
                .remainingAmount(remainingAmount)
                .emiCount(request.getEmiCount())
                .installmentNumber(instNum)
                .paymentDate(LocalDate.now())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "FULL")
                .transactionId(transactionId)
                .receiptNumber(receiptNumber)
                .status(paymentStatus)
                .dueDate(dueDate)
                .build();

        payment = paymentRepository.save(payment);

        // If recorded directly as PAID, activate enrollment and send receipt email
        if (paymentStatus == PaymentStatus.PAID) {
            payment.setPaidDate(LocalDate.now());
            if (enrollment != null) {
                enrollment.setStatus(com.skillsphere.common.enums.EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(enrollment);
            }
            emailService.sendPaymentReceiptEmail(student.getUser().getEmail(), student.getUser().getName(),
                    course.getName(), payment.getAmount(), payment.getReceiptNumber(), payment.getTransactionId());
        }

        // Notify student in-app
        notificationService.createNotification(student.getUser(), "Payment Recorded",
                "Payment of Rs. " + payment.getAmount() + " for course '" + course.getName() + "' has been recorded. Status: " + paymentStatus + ", Receipt: " + receiptNumber, "PAYMENT");

        // Audit Log
        auditLogService.log("PAYMENT_RECORD", "Payment", payment.getId().toString(),
                student.getUser().getId(), student.getUser().getEmail(), student.getUser().getRole().name(),
                "Payment recorded: Rs. " + payment.getAmount() + " for " + course.getName() + " (Status: " + paymentStatus + ")", null);

        return mapToResponse(payment);
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(Long id, PaymentStatus status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        PaymentStatus oldStatus = payment.getStatus();
        payment.setStatus(status);

        if (status == PaymentStatus.PAID) {
            payment.setPaidDate(LocalDate.now());
            payment.setPaymentDate(LocalDate.now());
            
            if (payment.getReceiptNumber() == null || payment.getReceiptNumber().isEmpty()) {
                payment.setReceiptNumber(generateReceiptNumber());
            }

            // Ensure enrollment is activated
            if (payment.getEnrollment() != null) {
                payment.getEnrollment().setStatus(com.skillsphere.common.enums.EnrollmentStatus.ACTIVE);
                enrollmentRepository.save(payment.getEnrollment());
            } else {
                java.util.Optional<com.skillsphere.batch.entity.Enrollment> enrOpt = enrollmentRepository.findByStudentIdAndCourseId(payment.getStudent().getId(), payment.getCourse().getId());
                if (enrOpt.isPresent()) {
                    com.skillsphere.batch.entity.Enrollment enr = enrOpt.get();
                    enr.setStatus(com.skillsphere.common.enums.EnrollmentStatus.ACTIVE);
                    enrollmentRepository.save(enr);
                    payment.setEnrollment(enr);
                }
            }
        }

        payment = paymentRepository.save(payment);

        // Auto-generate next installment if PAID
        if (status == PaymentStatus.PAID && oldStatus != PaymentStatus.PAID) {
            BigDecimal totalCourseFee = payment.getTotalCourseFee() != null ? payment.getTotalCourseFee() : 
                    (payment.getCourse() != null && payment.getCourse().getFee() != null ? payment.getCourse().getFee() : BigDecimal.ZERO);
            payment.setTotalCourseFee(totalCourseFee);

            BigDecimal currentPaid = payment.getAmountPaid() != null ? payment.getAmountPaid() : (payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO);
            payment.setAmountPaid(currentPaid);

            BigDecimal remaining = payment.getRemainingAmount() != null ? payment.getRemainingAmount() : totalCourseFee.subtract(currentPaid);
            if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;
            payment.setRemainingAmount(remaining);
            payment = paymentRepository.save(payment);

            if ("EMI".equalsIgnoreCase(payment.getPaymentMethod()) && payment.getEmiCount() != null && payment.getInstallmentNumber() != null) {
                final Long studentId = payment.getStudent().getId();
                final Long courseId = payment.getCourse().getId();
                final int nextInstNum = payment.getInstallmentNumber() + 1;
                if (nextInstNum <= payment.getEmiCount()) {
                    boolean exists = paymentRepository.findByStudentId(studentId).stream()
                            .anyMatch(p -> p.getCourse().getId().equals(courseId) 
                                    && "EMI".equalsIgnoreCase(p.getPaymentMethod())
                                    && p.getInstallmentNumber() != null
                                    && p.getInstallmentNumber() == nextInstNum);
                    
                    if (!exists) {
                        int emis = payment.getEmiCount() > 0 ? payment.getEmiCount() : 1;
                        BigDecimal nextAmount = totalCourseFee.divide(BigDecimal.valueOf(emis), 2, java.math.RoundingMode.HALF_UP);
                        LocalDate nextDueDate = payment.getDueDate() != null ? payment.getDueDate().plusMonths(1) : LocalDate.now().plusMonths(1);
                        BigDecimal nextRemaining = remaining.subtract(nextAmount);
                        if (nextRemaining.compareTo(BigDecimal.ZERO) < 0) {
                            nextRemaining = BigDecimal.ZERO;
                        }

                        Payment nextInstallment = Payment.builder()
                                .student(payment.getStudent())
                                .course(payment.getCourse())
                                .enrollment(payment.getEnrollment())
                                .amount(nextAmount)
                                .totalCourseFee(totalCourseFee)
                                .amountPaid(currentPaid.add(nextAmount))
                                .remainingAmount(nextRemaining)
                                .emiCount(payment.getEmiCount())
                                .installmentNumber(nextInstNum)
                                .paymentMethod("EMI")
                                .status(PaymentStatus.PENDING)
                                .dueDate(nextDueDate)
                                .receiptNumber(generateReceiptNumber())
                                .transactionId("TXN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase())
                                .build();
                        paymentRepository.save(nextInstallment);

                        notificationService.createNotification(payment.getStudent().getUser(), "Next EMI Installment Scheduled",
                                "EMI Installment #" + nextInstNum + " of Rs. " + nextAmount + " is scheduled. Due Date: " + nextDueDate, "PAYMENT");
                    }
                }
            }

            // Send receipt email
            emailService.sendPaymentReceiptEmail(payment.getStudent().getUser().getEmail(), payment.getStudent().getUser().getName(),
                    payment.getCourse().getName(), payment.getAmount(), payment.getReceiptNumber(), payment.getTransactionId());

            // Audit Log
            auditLogService.log("PAYMENT_SUCCESS", "Payment", payment.getId().toString(),
                    payment.getStudent().getUser().getId(), payment.getStudent().getUser().getEmail(), "ROLE_STUDENT",
                    "Payment of Rs. " + payment.getAmount() + " approved/paid for " + payment.getCourse().getName(), null);
        }

        notificationService.createNotification(payment.getStudent().getUser(), "Payment Status Updated",
                "Your payment of Rs. " + payment.getAmount() + " for course '" + payment.getCourse().getName() + "' is now " + status + ".", "PAYMENT");

        return mapToResponse(payment);
    }

    @Transactional
    public void checkOverdueEmis() {
        try {
            List<Payment> pendingPayments = paymentRepository.findAll().stream()
                    .filter(p -> (p.getStatus() == PaymentStatus.PENDING || p.getStatus() == PaymentStatus.OVERDUE) && "EMI".equalsIgnoreCase(p.getPaymentMethod()) && p.getDueDate() != null)
                    .collect(Collectors.toList());
                    
            LocalDate today = LocalDate.now();
            for (Payment p : pendingPayments) {
                if (p.getDueDate().isBefore(today)) {
                    if (p.getStatus() != PaymentStatus.OVERDUE) {
                        p.setStatus(PaymentStatus.OVERDUE);
                        paymentRepository.save(p);
                    }
                    
                    String notifTitle = "EMI Payment Overdue";
                    String notifMsg = "Your EMI installment of Rs. " + p.getAmount() + " was due on " + p.getDueDate() + " and is now OVERDUE.";
                    
                    boolean alreadyNotified = notificationRepository.findByUserIdOrderByCreatedAtDesc(p.getStudent().getUser().getId()).stream()
                            .anyMatch(n -> n.getTitle().equals(notifTitle) && n.getMessage().contains("due on " + p.getDueDate()) && n.getCreatedAt().toLocalDate().isEqual(today));
                    
                    if (!alreadyNotified) {
                        notificationService.createNotification(p.getStudent().getUser(), notifTitle, notifMsg, "EMI_OVERDUE");
                    }
                }
            }
        } catch (Exception e) {
            // Non-blocking catch
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment p = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        return mapToResponse(p);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getStudentPayments(Long studentUserId) {
        Student student = studentRepository.findByUserId(studentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found for user: " + studentUserId));
        return paymentRepository.findByStudentId(student.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private String generateReceiptNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String rand = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "RCPT-" + datePart + "-" + rand;
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .studentId(p.getStudent().getId())
                .studentName(p.getStudent().getUser() != null ? p.getStudent().getUser().getName() : "Student")
                .registrationNumber(p.getStudent().getRegistrationNumber())
                .courseId(p.getCourse().getId())
                .courseName(p.getCourse().getName())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate())
                .paymentMethod(p.getPaymentMethod())
                .transactionId(p.getTransactionId())
                .receiptNumber(p.getReceiptNumber() != null ? p.getReceiptNumber() : "RCPT-" + p.getId())
                .status(p.getStatus().name())
                .totalCourseFee(p.getTotalCourseFee())
                .amountPaid(p.getAmountPaid())
                .remainingAmount(p.getRemainingAmount())
                .emiCount(p.getEmiCount())
                .installmentNumber(p.getInstallmentNumber())
                .dueDate(p.getDueDate())
                .paidDate(p.getPaidDate())
                .build();
    }
}
