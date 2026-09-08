package com.skillsphere.course.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialResponse {
    private Long id;
    private Long courseId;
    private String courseName;
    private String title;
    private String description;
    private String url;
    private String type;
    private LocalDateTime createdAt;
}
