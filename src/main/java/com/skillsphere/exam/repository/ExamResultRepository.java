package com.skillsphere.exam.repository;

import com.skillsphere.exam.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudentId(Long studentId);
    List<ExamResult> findByExamId(Long examId);
    Optional<ExamResult> findByExamIdAndStudentId(Long examId, Long studentId);
}
