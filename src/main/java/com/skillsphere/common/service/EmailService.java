package com.skillsphere.common.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:mohanraj.k1110@gmail.com}")
    private String fromEmail;

    @Value("${app.mail.from-name:SkillSphere Learning Portal}")
    private String fromName;

    public void sendOtpEmail(String toEmail, String name, String otp) {
        String subject = "SkillSphere - Password Reset Verification Code";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #4f46e5; text-align: center;">SkillSphere Password Reset</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>We received a request to reset your password for your SkillSphere account. Use the verification code below to proceed:</p>
                    <div style="text-align: center; margin: 30px 0;">
                        <span style="font-size: 32px; font-weight: bold; letter-spacing: 6px; color: #4f46e5; background: #f3f4f6; padding: 12px 24px; border-radius: 6px; border: 1px dashed #4f46e5;">%s</span>
                    </div>
                    <p style="color: #6b7280; font-size: 14px;">This code is valid for <strong>10 minutes</strong>. If you did not request a password reset, please ignore this email or contact your administrator immediately.</p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                    <p style="font-size: 12px; color: #9ca3af; text-align: center;">SkillSphere Enterprise Learning Platform &copy; 2026</p>
                </div>
                """.formatted(name != null ? name : "User", otp);

        sendEmail(toEmail, subject, content);
    }

    public void sendWelcomeEmail(String toEmail, String name, String role) {
        String subject = "Welcome to SkillSphere Learning Platform!";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #4f46e5; text-align: center;">Welcome to SkillSphere!</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Your account has been successfully registered on the SkillSphere Learning Management Platform with the role: <strong>%s</strong>.</p>
                    <p>You can now log in using your registered email address:</p>
                    <div style="text-align: center; margin: 25px 0;">
                        <a href="http://localhost:8080/login" style="background-color: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">Log In to Your Dashboard</a>
                    </div>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                    <p style="font-size: 12px; color: #9ca3af; text-align: center;">SkillSphere Enterprise Learning Platform &copy; 2026</p>
                </div>
                """.formatted(name, role);

        sendEmail(toEmail, subject, content);
    }

    public void sendPaymentReceiptEmail(String toEmail, String name, String courseName, BigDecimal amount, String receiptNumber, String txnId) {
        String subject = "SkillSphere Payment Receipt - " + receiptNumber;
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #10b981; text-align: center;">Payment Receipt Confirmed</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Thank you for your payment. Here is the summary of your transaction:</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Receipt Number:</td><td style="padding: 8px; font-weight: bold;">%s</td></tr>
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Course:</td><td style="padding: 8px; font-weight: bold;">%s</td></tr>
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Amount Paid:</td><td style="padding: 8px; font-weight: bold; color: #10b981;">Rs. %s</td></tr>
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Transaction ID:</td><td style="padding: 8px;">%s</td></tr>
                        <tr><td style="padding: 8px; color: #6b7280;">Status:</td><td style="padding: 8px; color: #10b981; font-weight: bold;">PAID / SUCCESS</td></tr>
                    </table>
                    <p style="color: #6b7280; font-size: 14px;">You can view and print your full official receipt from your student dashboard under the Payments section.</p>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                    <p style="font-size: 12px; color: #9ca3af; text-align: center;">SkillSphere Enterprise Learning Platform &copy; 2026</p>
                </div>
                """.formatted(name, receiptNumber, courseName, amount, txnId);

        sendEmail(toEmail, subject, content);
    }

    public void sendCertificateNotificationEmail(String toEmail, String name, String courseName, String certNumber, String verificationCode, String grade, Double score) {
        String subject = "Congratulations! Your Certificate for " + courseName + " is Ready!";
        String gradeStr = (grade != null && !grade.isEmpty()) ? grade : "PASSED";
        String scoreStr = (score != null) ? String.format("%.1f%%", score) : "Satisfactory";
        String content = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px;">
                    <h2 style="color: #f59e0b; text-align: center;">Certificate of Completion</h2>
                    <p>Hello <strong>%s</strong>,</p>
                    <p>Congratulations on successfully completing the course <strong>%s</strong>! Your official certificate has been issued with distinction.</p>
                    <table style="width: 100%%; border-collapse: collapse; margin: 20px 0;">
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Certificate Number:</td><td style="padding: 8px; font-weight: bold;">%s</td></tr>
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Assigned Grade:</td><td style="padding: 8px; font-weight: bold; color: #10b981;">%s (%s)</td></tr>
                        <tr style="border-bottom: 1px solid #e5e7eb;"><td style="padding: 8px; color: #6b7280;">Verification Code:</td><td style="padding: 8px; font-weight: bold; color: #4f46e5;">%s</td></tr>
                    </table>
                    <div style="text-align: center; margin: 25px 0;">
                        <a href="http://localhost:8080/student/certificates" style="background-color: #f59e0b; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; margin-right: 10px;">Download Certificate</a>
                        <a href="http://localhost:8080/verify-certificate" style="background-color: #4f46e5; color: white; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold;">Public Verify</a>
                    </div>
                    <hr style="border: none; border-top: 1px solid #e5e7eb; margin: 20px 0;">
                    <p style="font-size: 12px; color: #9ca3af; text-align: center;">SkillSphere Enterprise Learning Platform &copy; 2026</p>
                </div>
                """.formatted(name, courseName, certNumber, gradeStr, scoreStr, verificationCode);

        sendEmail(toEmail, subject, content);
    }

    public void sendEmail(String to, String subject, String htmlContent) {
        // Run synchronously with try-catch so failures are logged without blocking or crashing requests
        try {
            if (mailSender != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail, fromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                log.info("Email successfully dispatched to {}", to);
                return;
            }
        } catch (Exception e) {
            log.warn("Direct SMTP email dispatch to {} failed: {}. Logging email content to console.", to, e.getMessage());
        }

        // Fallback console logging for environments without live SMTP configuration
        log.info("""
                \n============================================================
                [SKILLSPHERE EMAIL SIMULATION]
                From: {} <{}>
                To: {}
                Subject: {}
                Body:
                {}
                ============================================================
                """, fromName, fromEmail, to, subject, htmlContent.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim());
    }
}
