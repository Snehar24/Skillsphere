package com.skillsphere.assignment.repository;

import com.skillsphere.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByBatchId(Long batchId);
    List<Assignment> findByTrainerId(Long trainerId);
    long countByStatus(String status);
}
