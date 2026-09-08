package com.skillsphere.project.entity;

import com.skillsphere.student.entity.Student;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "project_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    private String repositoryUrl;

    @Column(columnDefinition = "TEXT")
    private String documentation;

    private Integer marks;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String submissionDocument;

    @Column(columnDefinition = "TEXT")
    private String matchedKeywords;

    @Column(columnDefinition = "TEXT")
    private String missingKeywords;

    private Double percentage;

    private LocalDateTime evaluationDate;

    @Column(nullable = false)
    private String status = "SUBMITTED";

    @CreationTimestamp
    private LocalDateTime submittedAt;
}
