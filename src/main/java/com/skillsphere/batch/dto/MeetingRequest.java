package com.skillsphere.batch.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class MeetingRequest {
    private Long batchId;
    private String title;
    private LocalDate meetingDate;
    private LocalTime meetingTime;
    private String meetingLink;
}
