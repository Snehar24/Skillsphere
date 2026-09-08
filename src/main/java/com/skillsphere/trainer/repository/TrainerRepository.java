package com.skillsphere.trainer.repository;

import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainerRepository extends JpaRepository<Trainer, Long> {
    Optional<Trainer> findByUser(User user);
    Optional<Trainer> findByUserId(Long userId);
    boolean existsByEmployeeId(String employeeId);
}
