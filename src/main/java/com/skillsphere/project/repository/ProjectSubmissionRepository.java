package com.skillsphere.project.repository;

import com.skillsphere.project.entity.ProjectSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmission, Long> {
    List<ProjectSubmission> findByStudentId(Long studentId);
    List<ProjectSubmission> findByProjectId(Long projectId);
    java.util.Optional<ProjectSubmission> findByProjectIdAndStudentId(Long projectId, Long studentId);
}
