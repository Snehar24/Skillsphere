package com.skillsphere.evaluation.entity;

import com.skillsphere.batch.entity.Batch;
import com.skillsphere.student.entity.Student;
import com.skillsphere.trainer.entity.Trainer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    private Double technicalScore;
    private Double communicationScore;
    private Double attendanceScore;
    private Double assignmentScore;
    private Double projectScore;
    private Double overallScore;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    private String grade;

    @CreationTimestamp
    private LocalDateTime evaluatedAt;
}
