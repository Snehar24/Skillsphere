package com.skillsphere.batch.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponse {
    private Long id;
    private Long batchId;
    private String batchName;
    private String title;
    private LocalDate meetingDate;
    private LocalTime meetingTime;
    private String meetingLink;
    private LocalDateTime createdAt;
}
