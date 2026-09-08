package com.skillsphere.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String paymentMethod;
    private String transactionId;
    private String status;
    private Integer emiCount;
    private Integer installmentNumber;
    private java.time.LocalDate dueDate;
    private java.time.LocalDate paidDate;
}
