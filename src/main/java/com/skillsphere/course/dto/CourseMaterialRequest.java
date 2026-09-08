package com.skillsphere.course.dto;

import lombok.Data;

@Data
public class CourseMaterialRequest {
    private String title;
    private String description;
    private String url;
    private String type;
}
