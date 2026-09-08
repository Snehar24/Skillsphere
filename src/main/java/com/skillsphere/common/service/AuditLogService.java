package com.skillsphere.common.service;

import com.skillsphere.common.dto.AuditLogResponse;
import com.skillsphere.common.entity.AuditLog;
import com.skillsphere.common.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(String action, String entityType, String entityId, Long userId, String userEmail, String userRole, String description, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .userId(userId)
                    .userEmail(userEmail)
                    .userRole(userRole)
                    .description(description)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to persist audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(String search, String action, Pageable pageable) {
        return getFilteredAuditLogs(search, action, null, null, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getFilteredAuditLogs(String search, String action, String userRole, String entityType,
                                                      String startDateStr, String endDateStr, Pageable pageable) {
        LocalDateTime start = null;
        LocalDateTime end = null;

        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
            try {
                start = LocalDate.parse(startDateStr.trim()).atStartOfDay();
            } catch (Exception ignored) {}
        }

        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
            try {
                end = LocalDate.parse(endDateStr.trim()).atTime(LocalTime.MAX);
            } catch (Exception ignored) {}
        }

        String searchVal = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String actionVal = (action != null && !action.trim().isEmpty()) ? action.trim() : null;
        String roleVal = (userRole != null && !userRole.trim().isEmpty()) ? userRole.trim() : null;
        String entityVal = (entityType != null && !entityType.trim().isEmpty()) ? entityType.trim() : null;

        return auditLogRepository.filterLogs(searchVal, actionVal, roleVal, entityVal, start, end, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAuditStats() {
        Map<String, Object> stats = new HashMap<>();
        long total = auditLogRepository.count();
        long today = auditLogRepository.countTodayEvents(LocalDate.now().atStartOfDay());
        long security = auditLogRepository.countSecurityEvents();
        long financial = auditLogRepository.countFinancialEvents();

        stats.put("totalEvents", total);
        stats.put("todayEvents", today);
        stats.put("securityEvents", security);
        stats.put("financialEvents", financial);
        return stats;
    }

    private AuditLogResponse mapToResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .action(a.getAction())
                .entityType(a.getEntityType())
                .entityId(a.getEntityId())
                .userId(a.getUserId())
                .userEmail(a.getUserEmail())
                .userRole(a.getUserRole())
                .description(a.getDescription())
                .ipAddress(a.getIpAddress())
                .timestamp(a.getTimestamp())
                .build();
    }
}
