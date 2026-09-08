package com.skillsphere.notification.service;

import com.skillsphere.common.service.AuditLogService;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.dto.AnnouncementRequest;
import com.skillsphere.notification.dto.AnnouncementResponse;
import com.skillsphere.notification.entity.Announcement;
import com.skillsphere.notification.repository.AnnouncementRepository;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public List<AnnouncementResponse> getPublicAnnouncements() {
        return announcementRepository.findPublicAnnouncements(LocalDate.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<AnnouncementResponse> getAnnouncementsForRole(String role) {
        return announcementRepository.findActiveAnnouncementsForRole(role, LocalDate.now()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public Page<AnnouncementResponse> getAllAnnouncements(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::mapToResponse);
    }

    public AnnouncementResponse getAnnouncementById(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        return mapToResponse(a);
    }

    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request, Long adminUserId) {
        User creator = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .message(request.getMessage())
                .priority(request.getPriority() != null ? request.getPriority().toUpperCase() : "MEDIUM")
                .targetRole(request.getTargetRole() != null ? request.getTargetRole().toUpperCase() : "ALL")
                .active(request.isActive())
                .expiryDate(request.getExpiryDate())
                .createdBy(creator)
                .build();

        announcement = announcementRepository.save(announcement);

        if (creator != null) {
            auditLogService.log("CREATE", "Announcement", announcement.getId().toString(),
                    creator.getId(), creator.getEmail(), creator.getRole().name(),
                    "Created announcement: " + announcement.getTitle(), null);
        }

        return mapToResponse(announcement);
    }

    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request, Long adminUserId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));

        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        if (request.getPriority() != null) announcement.setPriority(request.getPriority().toUpperCase());
        if (request.getTargetRole() != null) announcement.setTargetRole(request.getTargetRole().toUpperCase());
        announcement.setActive(request.isActive());
        announcement.setExpiryDate(request.getExpiryDate());

        Announcement saved = announcementRepository.save(announcement);

        if (adminUserId != null) {
            userRepository.findById(adminUserId).ifPresent(u ->
                auditLogService.log("UPDATE", "Announcement", id.toString(),
                        u.getId(), u.getEmail(), u.getRole().name(),
                        "Updated announcement: " + saved.getTitle(), null)
            );
        }

        return mapToResponse(saved);
    }

    @Transactional
    public void deleteAnnouncement(Long id, Long adminUserId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", id));
        String title = announcement.getTitle();
        announcementRepository.delete(announcement);

        if (adminUserId != null) {
            userRepository.findById(adminUserId).ifPresent(u ->
                auditLogService.log("DELETE", "Announcement", id.toString(),
                        u.getId(), u.getEmail(), u.getRole().name(),
                        "Deleted announcement: " + title, null)
            );
        }
    }

    private AnnouncementResponse mapToResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .message(a.getMessage())
                .priority(a.getPriority())
                .targetRole(a.getTargetRole())
                .active(a.isActive())
                .expiryDate(a.getExpiryDate())
                .createdByName(a.getCreatedBy() != null ? a.getCreatedBy().getName() : "System")
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
