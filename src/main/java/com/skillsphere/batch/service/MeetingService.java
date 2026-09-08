package com.skillsphere.batch.service;

import com.skillsphere.batch.dto.MeetingRequest;
import com.skillsphere.batch.dto.MeetingResponse;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.entity.Meeting;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.batch.repository.EnrollmentRepository;
import com.skillsphere.batch.repository.MeetingRepository;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final BatchRepository batchRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    @Transactional
    public MeetingResponse scheduleMeeting(MeetingRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        Meeting meeting = Meeting.builder()
                .batch(batch)
                .title(request.getTitle())
                .meetingDate(request.getMeetingDate())
                .meetingTime(request.getMeetingTime())
                .meetingLink(request.getMeetingLink())
                .build();

        meeting = meetingRepository.save(meeting);

        // Notify enrolled students in batch
        enrollmentRepository.findByBatchId(batch.getId()).forEach(enrollment -> {
            notificationService.createNotification(enrollment.getStudent().getUser(), "New Meeting Scheduled",
                    "A new class meeting '" + request.getTitle() + "' is scheduled for " + request.getMeetingDate() + " at " + request.getMeetingTime() + ". Link: " + request.getMeetingLink(), "MEETING");
        });

        return mapToResponse(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getBatchMeetings(Long batchId) {
        return meetingRepository.findByBatchId(batchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private MeetingResponse mapToResponse(Meeting m) {
        return MeetingResponse.builder()
                .id(m.getId())
                .batchId(m.getBatch().getId())
                .batchName(m.getBatch().getBatchName())
                .title(m.getTitle())
                .meetingDate(m.getMeetingDate())
                .meetingTime(m.getMeetingTime())
                .meetingLink(m.getMeetingLink())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
