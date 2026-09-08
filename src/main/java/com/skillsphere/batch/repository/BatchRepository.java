package com.skillsphere.batch.repository;

import com.skillsphere.batch.entity.Batch;
import com.skillsphere.common.enums.BatchStatus;
import com.skillsphere.trainer.entity.Trainer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByTrainer(Trainer trainer);
    List<Batch> findByStatus(BatchStatus status);
    long countByStatus(BatchStatus status);

    List<Batch> findByCourseId(Long courseId);
    List<Batch> findByCourseIdAndStatus(Long courseId, BatchStatus status);

    @Query("SELECT b FROM Batch b WHERE b.trainer.id = :trainerId")
    List<Batch> findByTrainerId(@Param("trainerId") Long trainerId);
}
