package com.skillsphere.student.service;

import com.skillsphere.common.enums.Role;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import com.skillsphere.student.dto.StudentRequest;
import com.skillsphere.student.dto.StudentResponse;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(String search, Pageable pageable) {
        Page<Student> studentsPage;
        if (search != null && !search.trim().isEmpty()) {
            studentsPage = studentRepository.searchStudents(search, pageable);
        } else {
            studentsPage = studentRepository.findAll(pageable);
        }
        return studentsPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByUserId(Long userId) {
        Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student with user id: " + userId));
        return mapToResponse(student);
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
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
                .role(Role.ROLE_STUDENT)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        user = userRepository.save(user);

        String regNumber = generateRegistrationNumber();
        Student student = Student.builder()
                .user(user)
                .registrationNumber(regNumber)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .qualification(request.getQualification())
                .address(request.getAddress())
                .joiningDate(LocalDate.now())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        student = studentRepository.save(student);

        notificationService.createNotification(user, "Welcome to SkillSphere",
                "Your student account has been created. Your registration number is " + regNumber, "SYSTEM");

        return mapToResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));

        User user = student.getUser();
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

        student.setDateOfBirth(request.getDateOfBirth());
        student.setGender(request.getGender());
        student.setQualification(request.getQualification());
        student.setAddress(request.getAddress());
        if (request.getStatus() != null) {
            student.setStatus(request.getStatus());
        }
        student = studentRepository.save(student);

        return mapToResponse(student);
    }

    @Transactional
    public void deleteStudent(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", id));
        User user = student.getUser();
        studentRepository.delete(student);
        userRepository.delete(user);
    }

    private StudentResponse mapToResponse(Student student) {
        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUser().getId())
                .name(student.getUser().getName())
                .email(student.getUser().getEmail())
                .phone(student.getUser().getPhone())
                .registrationNumber(student.getRegistrationNumber())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .qualification(student.getQualification())
                .address(student.getAddress())
                .joiningDate(student.getJoiningDate())
                .status(student.getStatus())
                .active(student.getUser().isActive())
                .build();
    }

    private String generateRegistrationNumber() {
        String prefix = "SS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String unique = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return prefix + unique;
    }
}
