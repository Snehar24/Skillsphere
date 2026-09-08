package com.skillsphere.auth.controller;

import com.skillsphere.auth.dto.LoginRequest;
import com.skillsphere.auth.dto.LoginResponse;
import com.skillsphere.auth.dto.StudentRegistrationRequest;
import com.skillsphere.auth.service.AuthService;
import com.skillsphere.exception.BadRequestException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        LoginResponse loginResponse = authService.login(request);

        // Store JWT in HttpOnly cookie
        Cookie cookie = new Cookie("ACCESS_TOKEN", loginResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(86400); // 24 hours
        // cookie.setSecure(true); // Enable in production with HTTPS
        response.addCookie(cookie);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping({"/register/student", "/student-register"})
    public ResponseEntity<Map<String, String>> registerStudent(
            @Valid @RequestBody StudentRegistrationRequest request) {
        String message = authService.registerStudent(request);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            throw new BadRequestException("Email is required");
        }
        String msg = authService.generateForgotPasswordOtp(email.trim());
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        if (email == null || otp == null) {
            throw new BadRequestException("Email and OTP code are required");
        }
        boolean valid = authService.verifyOtp(email.trim(), otp.trim());
        if (!valid) {
            throw new BadRequestException("Invalid or expired verification code");
        }
        return ResponseEntity.ok(Map.of("valid", true, "message", "Code verified successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        String newPassword = payload.get("newPassword");
        String confirmNewPassword = payload.get("confirmNewPassword");

        if (email == null || otp == null || newPassword == null || confirmNewPassword == null) {
            throw new BadRequestException("All fields are required");
        }

        String msg = authService.resetPasswordWithOtp(email.trim(), otp.trim(), newPassword, confirmNewPassword);
        return ResponseEntity.ok(Map.of("message", msg));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        // Clear the JWT cookie
        Cookie cookie = new Cookie("ACCESS_TOKEN", "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
