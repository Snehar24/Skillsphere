package com.skillsphere.exam.entity;

import com.skillsphere.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private Integer score;

    private Integer attemptNumber;

    private Integer totalQuestions;

    private Integer correctAnswers;

    private Integer wrongAnswers;

    private Integer maxMarks;

    private Double percentage;

    private String result; // PASS / FAIL

    private String submissionType; // MANUAL, TIMEOUT, FULLSCREEN_VIOLATION

    @Builder.Default
    private int violationCount = 0;

    @CreationTimestamp
    private LocalDateTime submittedAt;
}
