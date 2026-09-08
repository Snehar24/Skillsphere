package com.skillsphere.notification.controller;

import com.skillsphere.notification.dto.AnnouncementResponse;
import com.skillsphere.notification.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class PublicAnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/public")
    public ResponseEntity<List<AnnouncementResponse>> getPublicAnnouncements() {
        return ResponseEntity.ok(announcementService.getPublicAnnouncements());
    }
}
