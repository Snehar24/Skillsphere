package com.skillsphere.payment.entity;

import com.skillsphere.common.enums.PaymentStatus;
import com.skillsphere.course.entity.Course;
import com.skillsphere.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    private LocalDate paymentDate;

    private String paymentMethod;

    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id")
    private com.skillsphere.batch.entity.Enrollment enrollment;

    private BigDecimal totalCourseFee;

    private BigDecimal amountPaid;

    private BigDecimal remainingAmount;

    private Integer emiCount;

    private Integer installmentNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDate dueDate;

    private LocalDate paidDate;

    @Column(unique = true)
    private String receiptNumber;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
