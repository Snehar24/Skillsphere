package com.skillsphere.report.controller;

import com.skillsphere.report.dto.ReportResponse;
import com.skillsphere.report.dto.StudentPerformanceReportResponse;
import com.skillsphere.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ReportResponse> getReport() {
        return ResponseEntity.ok(reportService.generateReport());
    }

    @GetMapping("/students")
    public ResponseEntity<List<StudentPerformanceReportResponse>> getStudentPerformanceReports(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(reportService.getStudentPerformanceReports(courseId, batchId, search));
    }
}
