package com.skillsphere.certificate.entity;

import com.skillsphere.course.entity.Course;
import com.skillsphere.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String certificateNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private LocalDate issueDate;

    private Double score;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ISSUED";

    @Column(unique = true)
    private String verificationCode;

    private String grade;

    private Long trainerId;

    private String trainerName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
