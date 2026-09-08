package com.skillsphere.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AnnouncementRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private String priority = "MEDIUM"; // HIGH, MEDIUM, LOW

    private String targetRole = "ALL"; // ALL, ADMIN, TRAINER, STUDENT

    private boolean active = true;

    private LocalDate expiryDate;
}
