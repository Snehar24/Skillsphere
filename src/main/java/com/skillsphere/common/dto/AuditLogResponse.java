package com.skillsphere.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String action;
    private String entityType;
    private String entityId;
    private Long userId;
    private String userEmail;
    private String userRole;
    private String description;
    private String ipAddress;
    private LocalDateTime timestamp;
}
