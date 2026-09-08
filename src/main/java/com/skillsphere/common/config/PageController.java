package com.skillsphere.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles all server-rendered page routes.
 * Fixes "No static resource login", "No static resource ." errors.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/home")
    public String homePage() {
        return "home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/student-register")
    public String studentRegisterPage() {
        return "student-register";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "forgot-password";
    }

    @GetMapping("/verify-certificate")
    public String verifyCertificatePage() {
        return "verify-certificate";
    }

    @GetMapping("/announcements")
    public String announcementsPage() {
        return "announcements";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/students")
    public String adminStudents() {
        return "admin/students";
    }

    @GetMapping("/admin/trainers")
    public String adminTrainers() {
        return "admin/trainers";
    }

    @GetMapping("/admin/courses")
    public String adminCourses() {
        return "admin/courses";
    }

    @GetMapping("/admin/batches")
    public String adminBatches() {
        return "admin/batches";
    }

    @GetMapping("/admin/enrollments")
    public String adminEnrollments() {
        return "admin/enrollments";
    }

    @GetMapping("/admin/attendance")
    public String adminAttendance() {
        return "admin/attendance";
    }

    @GetMapping("/admin/assignments")
    public String adminAssignments() {
        return "admin/assignments";
    }

    @GetMapping("/admin/exams")
    public String adminExams() {
        return "admin/exams";
    }

    @GetMapping("/admin/projects")
    public String adminProjects() {
        return "admin/projects";
    }

    @GetMapping("/admin/evaluations")
    public String adminEvaluations() {
        return "admin/evaluations";
    }

    @GetMapping("/admin/certificates")
    public String adminCertificates() {
        return "admin/certificates";
    }

    @GetMapping("/admin/payments")
    public String adminPayments() {
        return "admin/payments";
    }

    @GetMapping("/admin/notifications")
    public String adminNotifications() {
        return "admin/notifications";
    }

    @GetMapping("/admin/reports")
    public String adminReports() {
        return "admin/reports";
    }

    @GetMapping("/admin/audit-logs")
    public String adminAuditLogs() {
        return "admin/audit-logs";
    }

    @GetMapping("/admin/users")
    public String adminUsers() {
        return "admin/users";
    }

    @GetMapping("/admin/announcements")
    public String adminAnnouncements() {
        return "admin/announcements";
    }

    @GetMapping("/trainer/dashboard")
    public String trainerDashboard() {
        return "trainer/dashboard";
    }

    @GetMapping("/trainer/batches")
    public String trainerBatches() {
        return "trainer/batches";
    }

    @GetMapping("/trainer/attendance")
    public String trainerAttendance() {
        return "trainer/attendance";
    }

    @GetMapping("/trainer/assignments")
    public String trainerAssignments() {
        return "trainer/assignments";
    }

    @GetMapping("/trainer/exams")
    public String trainerExams() {
        return "trainer/exams";
    }

    @GetMapping("/trainer/projects")
    public String trainerProjects() {
        return "trainer/projects";
    }

    @GetMapping("/trainer/evaluations")
    public String trainerEvaluations() {
        return "trainer/evaluations";
    }

    @GetMapping("/student/dashboard")
    public String studentDashboard() {
        return "student/dashboard";
    }

    @GetMapping("/student/assignments")
    public String studentAssignments() {
        return "student/assignments";
    }

    @GetMapping("/student/exams")
    public String studentExams() {
        return "student/exams";
    }

    @GetMapping("/student/projects")
    public String studentProjects() {
        return "student/projects";
    }

    @GetMapping("/student/certificates")
    public String studentCertificates() {
        return "student/certificates";
    }

    @GetMapping("/student/payments")
    public String studentPayments() {
        return "student/payments";
    }

    @GetMapping("/student/attendance")
    public String studentAttendance() {
        return "student/attendance";
    }

    @GetMapping("/student/payment-checkout")
    public String studentPaymentCheckout() {
        return "student/payment-checkout";
    }
}
