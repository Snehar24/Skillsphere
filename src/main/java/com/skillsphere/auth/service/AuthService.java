package com.skillsphere.auth.service;

import com.skillsphere.auth.dto.LoginRequest;
import com.skillsphere.auth.dto.LoginResponse;
import com.skillsphere.auth.dto.StudentRegistrationRequest;
import com.skillsphere.auth.entity.PasswordResetToken;
import com.skillsphere.auth.repository.PasswordResetTokenRepository;
import com.skillsphere.common.enums.Role;
import com.skillsphere.common.service.AuditLogService;
import com.skillsphere.common.service.EmailService;
import com.skillsphere.exception.BadRequestException;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.security.JwtService;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (!user.isActive()) {
            throw new BadRequestException("Your account has been deactivated. Please contact support.");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());

        String redirectUrl = switch (user.getRole()) {
            case ROLE_ADMIN -> "/admin/dashboard";
            case ROLE_TRAINER -> "/trainer/dashboard";
            case ROLE_STUDENT -> "/student/dashboard";
        };

        // Audit Log
        auditLogService.log("LOGIN", "User", user.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "User logged in successfully", null);

        return LoginResponse.builder()
                .token(token)
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole().name())
                .userId(user.getId())
                .redirectUrl(redirectUrl)
                .message("Login successful")
                .build();
    }

    @Transactional
    public String registerStudent(StudentRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.ROLE_STUDENT)
                .active(true)
                .build();
        user = userRepository.save(user);

        String regNumber = generateRegistrationNumber();

        Student student = Student.builder()
                .user(user)
                .registrationNumber(regNumber)
                .qualification(request.getQualification())
                .joiningDate(LocalDate.now())
                .status("ACTIVE")
                .build();
        studentRepository.save(student);

        // Send welcome email
        emailService.sendWelcomeEmail(user.getEmail(), user.getName(), "Student");

        // Audit log
        auditLogService.log("STUDENT_REGISTER", "Student", student.getId().toString(),
                user.getId(), user.getEmail(), "ROLE_STUDENT", "Student self-registered with Reg# " + regNumber, null);

        log.info("Student registered: {} with reg# {}", user.getEmail(), regNumber);
        return "Student registered successfully. Registration number: " + regNumber;
    }

    @Transactional
    public String generateForgotPasswordOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account registered with email: " + email));

        // Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1000000));

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .otp(otp)
                .expiryTime(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Send OTP email
        emailService.sendOtpEmail(user.getEmail(), user.getName(), otp);

        auditLogService.log("OTP_GENERATE", "PasswordResetToken", resetToken.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "Generated password reset OTP", null);

        return "Verification code has been sent to " + email;
    }

    @Transactional(readOnly = true)
    public boolean verifyOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return passwordResetTokenRepository
                .findByUserAndOtpAndUsedFalseAndExpiryTimeAfter(user, otp, LocalDateTime.now())
                .isPresent();
    }

    @Transactional
    public String resetPasswordWithOtp(String email, String otp, String newPassword, String confirmNewPassword) {
        if (!newPassword.equals(confirmNewPassword)) {
            throw new BadRequestException("New passwords do not match");
        }
        if (newPassword.length() < 6) {
            throw new BadRequestException("Password must be at least 6 characters");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        PasswordResetToken token = passwordResetTokenRepository
                .findByUserAndOtpAndUsedFalseAndExpiryTimeAfter(user, otp, LocalDateTime.now())
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code"));

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.log("PASSWORD_RESET_OTP", "User", user.getId().toString(),
                user.getId(), user.getEmail(), user.getRole().name(), "Reset password using OTP", null);

        return "Password has been successfully reset. You can now log in.";
    }

    private String generateRegistrationNumber() {
        String prefix = "SS" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String unique = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return prefix + unique;
    }
}
