package com.skillsphere.batch.entity;

import com.skillsphere.common.enums.BatchStatus;
import com.skillsphere.course.entity.Course;
import com.skillsphere.trainer.entity.Trainer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "batches")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Batch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String batchName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trainer_id")
    private Trainer trainer;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private int maxStudents = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status = BatchStatus.UPCOMING;

    private String meetingLink;

    private String meetingTitle;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
