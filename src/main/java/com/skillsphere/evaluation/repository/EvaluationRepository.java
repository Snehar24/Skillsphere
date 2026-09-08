package com.skillsphere.evaluation.repository;

import com.skillsphere.evaluation.entity.Evaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    List<Evaluation> findByStudentId(Long studentId);
    List<Evaluation> findByTrainerId(Long trainerId);
    List<Evaluation> findByStudentIdAndBatchId(Long studentId, Long batchId);

    @org.springframework.data.jpa.repository.Query("SELECT e FROM Evaluation e WHERE e.student.id = :studentId AND e.batch.course.id = :courseId")
    List<Evaluation> findByStudentIdAndCourseId(@org.springframework.data.repository.query.Param("studentId") Long studentId, @org.springframework.data.repository.query.Param("courseId") Long courseId);
}
