package com.skillsphere.project.repository;

import com.skillsphere.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByBatchId(Long batchId);
    List<Project> findByTrainerId(Long trainerId);
}
