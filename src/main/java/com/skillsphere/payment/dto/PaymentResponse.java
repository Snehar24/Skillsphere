package com.skillsphere.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long studentId;
    private String studentName;
    private String registrationNumber;
    private Long courseId;
    private String courseName;
    private BigDecimal amount;
    private LocalDate paymentDate;
    private String paymentMethod;
    private String transactionId;
    private String status;
    private BigDecimal totalCourseFee;
    private BigDecimal amountPaid;
    private BigDecimal remainingAmount;
    private Integer emiCount;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private LocalDate paidDate;
    private String receiptNumber;
}
