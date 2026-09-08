package com.skillsphere.exam.entity;

import com.skillsphere.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private Integer attemptNumber;

    @Column(nullable = false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Integer score;

    private Integer maxMarks;

    private Double percentage;

    private String result; // PASS / FAIL

    @Column(columnDefinition = "TEXT")
    private String answersJson; // JSON representation of student answers

    private boolean submitted;

    @Builder.Default
    private int violationCount = 0;

    private String submissionType; // MANUAL, TIMEOUT, FULLSCREEN_VIOLATION
}
