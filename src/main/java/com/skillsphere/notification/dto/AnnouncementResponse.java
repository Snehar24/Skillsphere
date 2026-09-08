package com.skillsphere.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String message;
    private String priority;
    private String targetRole;
    private boolean active;
    private LocalDate expiryDate;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
