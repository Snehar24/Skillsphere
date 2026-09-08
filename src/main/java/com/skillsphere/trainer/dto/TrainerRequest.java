package com.skillsphere.trainer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TrainerRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String password; // Optional on update, required on create

    private String phone;
    private String employeeId;
    private String specialization;
    private Integer experience;
    private String qualification;
    private String status;
    private Boolean active;
}
