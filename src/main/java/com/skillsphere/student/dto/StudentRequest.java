package com.skillsphere.student.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password; // Optional on update, required on create

    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String qualification;
    private String address;
    private String status;
    private Boolean active;
}
