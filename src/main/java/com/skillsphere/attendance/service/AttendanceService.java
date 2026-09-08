package com.skillsphere.attendance.service;

import com.skillsphere.attendance.dto.AttendanceBulkRequest;
import com.skillsphere.attendance.dto.AttendanceResponse;
import com.skillsphere.attendance.entity.Attendance;
import com.skillsphere.attendance.repository.AttendanceRepository;
import com.skillsphere.batch.entity.Batch;
import com.skillsphere.batch.repository.BatchRepository;
import com.skillsphere.common.enums.AttendanceStatus;
import com.skillsphere.exception.ResourceNotFoundException;
import com.skillsphere.student.entity.Student;
import com.skillsphere.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final BatchRepository batchRepository;
    private final StudentRepository studentRepository;
    private final com.skillsphere.common.service.AuditLogService auditLogService;

    @Transactional
    public void markAttendance(AttendanceBulkRequest request) {
        Batch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch", request.getBatchId()));

        int count = 0;
        for (AttendanceBulkRequest.StudentAttendanceRecord rec : request.getRecords()) {
            Student student = studentRepository.findById(rec.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", rec.getStudentId()));

            // Find existing attendance for this student/batch/date to update, otherwise create new
            List<Attendance> existing = attendanceRepository.findByStudentIdAndBatchId(student.getId(), batch.getId());
            Attendance attendance = existing.stream()
                    .filter(a -> a.getDate().equals(request.getDate()))
                    .findFirst()
                    .orElse(Attendance.builder().student(student).batch(batch).date(request.getDate()).build());

            attendance.setStatus(AttendanceStatus.valueOf(rec.getStatus()));
            attendance.setRemarks(rec.getRemarks());
            attendanceRepository.save(attendance);
            count++;
        }

        String trainerEmail = (batch.getTrainer() != null && batch.getTrainer().getUser() != null) ? batch.getTrainer().getUser().getEmail() : "system";
        Long trainerUserId = (batch.getTrainer() != null && batch.getTrainer().getUser() != null) ? batch.getTrainer().getUser().getId() : null;
        auditLogService.log("ATTENDANCE_MARK", "Batch", batch.getId().toString(), trainerUserId, trainerEmail, "ROLE_TRAINER",
                String.format("Marked attendance for %d students in batch '%s' on %s", count, batch.getBatchName(), request.getDate()), null);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getBatchAttendance(Long batchId) {
        List<Attendance> attendances = attendanceRepository.findByBatchId(batchId);
        return attendances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentAttendanceSummary(Long studentId, Long batchId) {
        if (batchId == null) {
            return getStudentOverallAttendanceSummary(studentId);
        }
        long totalClasses = attendanceRepository.countByStudentIdAndBatchId(studentId, batchId);
        long present = attendanceRepository.countByStudentIdAndBatchIdAndStatus(studentId, batchId, AttendanceStatus.PRESENT);
        long late = attendanceRepository.countByStudentIdAndBatchIdAndStatus(studentId, batchId, AttendanceStatus.LATE);
        long absent = attendanceRepository.countByStudentIdAndBatchIdAndStatus(studentId, batchId, AttendanceStatus.ABSENT);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClasses", totalClasses);
        summary.put("present", present);
        summary.put("late", late);
        summary.put("absent", absent);
        if (totalClasses > 0) {
            double percentage = ((double)(present + late) / totalClasses) * 100.0;
            summary.put("percentage", Math.round(percentage * 100.0) / 100.0);
        } else {
            summary.put("percentage", 0.0);
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStudentOverallAttendanceSummary(Long studentId) {
        List<Attendance> all = attendanceRepository.findByStudentId(studentId);
        long totalClasses = all.size();
        long present = all.stream().filter(a -> a.getStatus() == AttendanceStatus.PRESENT).count();
        long late = all.stream().filter(a -> a.getStatus() == AttendanceStatus.LATE).count();
        long absent = all.stream().filter(a -> a.getStatus() == AttendanceStatus.ABSENT).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalClasses", totalClasses);
        summary.put("present", present);
        summary.put("late", late);
        summary.put("absent", absent);
        if (totalClasses > 0) {
            double percentage = ((double)(present + late) / totalClasses) * 100.0;
            summary.put("percentage", Math.round(percentage * 100.0) / 100.0);
        } else {
            summary.put("percentage", 0.0);
        }
        return summary;
    }

    @Transactional(readOnly = true)
    public double getStudentAttendancePercentage(Long studentId, Long batchId) {
        List<Attendance> records = (batchId != null) ?
                attendanceRepository.findByStudentIdAndBatchId(studentId, batchId) :
                attendanceRepository.findByStudentId(studentId);
        if (records.isEmpty()) return 0.0;
        long attended = records.stream()
                .filter(a -> a.getStatus() == AttendanceStatus.PRESENT || a.getStatus() == AttendanceStatus.LATE)
                .count();
        double pct = ((double) attended / records.size()) * 100.0;
        return Math.round(pct * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getStudentAttendance(Long studentId, Long batchId) {
        List<Attendance> attendances;
        if (batchId != null) {
            attendances = attendanceRepository.findByStudentIdAndBatchId(studentId, batchId);
        } else {
            attendances = attendanceRepository.findByStudentId(studentId);
        }
        return attendances.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(Attendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getUser().getName())
                .registrationNumber(attendance.getStudent().getRegistrationNumber())
                .batchId(attendance.getBatch().getId())
                .batchName(attendance.getBatch().getBatchName())
                .date(attendance.getDate())
                .status(attendance.getStatus().name())
                .remarks(attendance.getRemarks())
                .build();
    }
}
