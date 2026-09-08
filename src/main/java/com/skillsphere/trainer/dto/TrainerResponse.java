package com.skillsphere.trainer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainerResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String employeeId;
    private String specialization;
    private Integer experience;
    private String qualification;
    private LocalDate joiningDate;
    private String status;
    private boolean active;
}
