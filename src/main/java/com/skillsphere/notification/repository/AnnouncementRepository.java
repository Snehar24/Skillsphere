package com.skillsphere.notification.repository;

import com.skillsphere.notification.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    List<Announcement> findByActiveTrueOrderByCreatedAtDesc();

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND " +
           "(a.expiryDate IS NULL OR a.expiryDate >= :today) AND " +
           "(a.targetRole = 'ALL' OR a.targetRole = :role) " +
           "ORDER BY CASE WHEN a.priority = 'HIGH' THEN 1 WHEN a.priority = 'MEDIUM' THEN 2 ELSE 3 END, a.createdAt DESC")
    List<Announcement> findActiveAnnouncementsForRole(@Param("role") String role, @Param("today") LocalDate today);

    @Query("SELECT a FROM Announcement a WHERE a.active = true AND " +
           "(a.expiryDate IS NULL OR a.expiryDate >= :today) " +
           "ORDER BY CASE WHEN a.priority = 'HIGH' THEN 1 WHEN a.priority = 'MEDIUM' THEN 2 ELSE 3 END, a.createdAt DESC")
    List<Announcement> findPublicAnnouncements(@Param("today") LocalDate today);

    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
