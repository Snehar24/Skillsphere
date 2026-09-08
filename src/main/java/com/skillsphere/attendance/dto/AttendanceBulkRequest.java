package com.skillsphere.attendance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class AttendanceBulkRequest {
    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Records list cannot be null")
    private List<StudentAttendanceRecord> records;

    @Data
    public static class StudentAttendanceRecord {
        @NotNull(message = "Student ID is required")
        private Long studentId;

        @NotNull(message = "Status is required")
        private String status; // PRESENT, ABSENT, LATE

        private String remarks;
    }
}
