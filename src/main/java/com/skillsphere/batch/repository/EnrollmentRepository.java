package com.skillsphere.batch.repository;

import com.skillsphere.batch.entity.Enrollment;
import com.skillsphere.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(Student student);
    List<Enrollment> findByStudentId(Long studentId);

    @Query("SELECT e FROM Enrollment e WHERE e.batch.id = :batchId")
    List<Enrollment> findByBatchId(@Param("batchId") Long batchId);

    Optional<Enrollment> findByStudentIdAndBatchId(Long studentId, Long batchId);
    boolean existsByStudentIdAndBatchId(Long studentId, Long batchId);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.batch.id = :batchId AND e.status = 'ACTIVE'")
    long countActiveEnrollmentsByBatchId(@Param("batchId") Long batchId);
}
