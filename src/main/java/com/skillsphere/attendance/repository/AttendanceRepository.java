package com.skillsphere.attendance.repository;

import com.skillsphere.attendance.entity.Attendance;
import com.skillsphere.common.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.batch.id = :batchId")
    List<Attendance> findByStudentIdAndBatchId(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.batch.id = :batchId AND a.status = :status")
    long countByStudentIdAndBatchIdAndStatus(@Param("studentId") Long studentId, @Param("batchId") Long batchId, @Param("status") AttendanceStatus status);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.batch.id = :batchId")
    long countByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.id = :studentId AND a.batch.id = :batchId")
    long countByStudentIdAndBatchId(@Param("studentId") Long studentId, @Param("batchId") Long batchId);

    List<Attendance> findByBatchId(Long batchId);
    
    List<Attendance> findByStudentId(Long studentId);
}
