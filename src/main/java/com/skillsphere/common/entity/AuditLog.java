package com.skillsphere.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String action; // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, PASSWORD_CHANGE, ROLE_CHANGE, ENROLLMENT, PAYMENT, EXAM_SUBMIT, CERTIFICATE_ISSUE

    private String entityType; // User, Course, Batch, Payment, Certificate, Exam, Assignment, Project

    private String entityId;

    private Long userId;

    private String userEmail;

    private String userRole;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
