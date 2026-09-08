package com.skillsphere.student.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private String registrationNumber;
    private LocalDate dateOfBirth;
    private String gender;
    private String qualification;
    private String address;
    private LocalDate joiningDate;
    private String status;
    private boolean active;
}
