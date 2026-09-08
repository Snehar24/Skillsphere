package com.skillsphere.trainer.service;

import com.skillsphere.common.enums.Role;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.trainer.dto.TrainerRequest;
import com.skillsphere.trainer.dto.TrainerResponse;
import com.skillsphere.trainer.entity.Trainer;
import com.skillsphere.trainer.repository.TrainerRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrainerService {

    private final UserRepository userRepository;
    private final TrainerRepository trainerRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<TrainerResponse> getAllTrainers(Pageable pageable) {
        return trainerRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public TrainerResponse getTrainerById(Long id) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", id));
        return mapToResponse(trainer);
    }

    @Transactional(readOnly = true)
    public TrainerResponse getTrainerByUserId(Long userId) {
        Trainer trainer = trainerRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer with user id: " + userId));
        return mapToResponse(trainer);
    }

    @Transactional
    public TrainerResponse createTrainer(TrainerRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password is required for creation");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.ROLE_TRAINER)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        user = userRepository.save(user);

        String employeeId = request.getEmployeeId();
        if (employeeId == null || employeeId.trim().isEmpty()) {
            employeeId = "TR" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } else if (trainerRepository.existsByEmployeeId(employeeId)) {
            throw new DuplicateResourceException("Employee ID already in use: " + employeeId);
        }

        Trainer trainer = Trainer.builder()
                .user(user)
                .employeeId(employeeId)
                .specialization(request.getSpecialization())
                .experience(request.getExperience())
                .qualification(request.getQualification())
                .joiningDate(LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        trainer = trainerRepository.save(trainer);

        notificationService.createNotification(user, "Welcome to SkillSphere",
                "Your trainer account has been created. Your Employee ID is " + employeeId, "SYSTEM");

        return mapToResponse(trainer);
    }

    @Transactional
    public TrainerResponse updateTrainer(Long id, TrainerRequest request) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", id));

        User user = trainer.getUser();
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already in use: " + request.getEmail());
        }

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);

        trainer.setSpecialization(request.getSpecialization());
        trainer.setExperience(request.getExperience());
        trainer.setQualification(request.getQualification());
        if (request.getStatus() != null) {
            trainer.setStatus(request.getStatus());
        }
        trainer = trainerRepository.save(trainer);

        return mapToResponse(trainer);
    }

    @Transactional
    public void deleteTrainer(Long id) {
        Trainer trainer = trainerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trainer", id));
        User user = trainer.getUser();
        trainerRepository.delete(trainer);
        userRepository.delete(user);
    }

    private TrainerResponse mapToResponse(Trainer trainer) {
        return TrainerResponse.builder()
                .id(trainer.getId())
                .userId(trainer.getUser().getId())
                .name(trainer.getUser().getName())
                .email(trainer.getUser().getEmail())
                .phone(trainer.getUser().getPhone())
                .employeeId(trainer.getEmployeeId())
                .specialization(trainer.getSpecialization())
                .experience(trainer.getExperience())
                .qualification(trainer.getQualification())
                .joiningDate(trainer.getJoiningDate())
                .status(trainer.getStatus())
                .active(trainer.getUser().isActive())
                .build();
    }
}
