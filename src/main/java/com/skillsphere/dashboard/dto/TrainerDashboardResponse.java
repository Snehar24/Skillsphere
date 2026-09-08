package com.skillsphere.dashboard.dto;

import com.skillsphere.batch.dto.BatchResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerDashboardResponse {
    private int batchCount;
    private long studentCount;
    private long courseCount;
    private long assignmentCount;
    private long examCount;
    private long projectCount;

    private List<BatchResponse> assignedBatches;
}
