package com.skillsphere.common.repository;

import com.skillsphere.common.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findTop50ByOrderByTimestampDesc();

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.entityId) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.userRole) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.ipAddress) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR a.action = :action) AND " +
           "(:userRole IS NULL OR :userRole = '' OR a.userRole = :userRole) AND " +
           "(:entityType IS NULL OR :entityType = '' OR a.entityType = :entityType) AND " +
           "(:startDate IS NULL OR a.timestamp >= :startDate) AND " +
           "(:endDate IS NULL OR a.timestamp <= :endDate)")
    Page<AuditLog> filterLogs(@Param("search") String search,
                              @Param("action") String action,
                              @Param("userRole") String userRole,
                              @Param("entityType") String entityType,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate,
                              Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.entityType) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.userEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.description) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:action IS NULL OR :action = '' OR a.action = :action)")
    Page<AuditLog> searchLogs(@Param("search") String search, @Param("action") String action, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.timestamp >= :startOfDay")
    long countTodayEvents(@Param("startOfDay") LocalDateTime startOfDay);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action LIKE 'LOGIN%' OR a.action LIKE 'PASSWORD%' OR a.action LIKE '%ROLE%'")
    long countSecurityEvents();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.action LIKE 'PAYMENT%' OR a.action LIKE 'REFUND%'")
    long countFinancialEvents();
}
