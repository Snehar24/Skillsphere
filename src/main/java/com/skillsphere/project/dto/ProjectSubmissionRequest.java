package com.skillsphere.project.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProjectSubmissionRequest {
    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    private String documentation;
}
