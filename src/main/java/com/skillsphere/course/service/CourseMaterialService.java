package com.skillsphere.course.service;

import com.skillsphere.course.dto.CourseMaterialRequest;
import com.skillsphere.course.dto.CourseMaterialResponse;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.entity.CourseMaterial;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.course.repository.CourseMaterialRepository;
import com.skillsphere.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseMaterialService {

    private final CourseMaterialRepository courseMaterialRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CourseMaterialResponse addMaterial(Long courseId, CourseMaterialRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));

        CourseMaterial material = CourseMaterial.builder()
                .course(course)
                .title(request.getTitle())
                .description(request.getDescription())
                .url(request.getUrl())
                .type(request.getType() != null ? request.getType() : "LINK")
                .build();

        material = courseMaterialRepository.save(material);
        return mapToResponse(material);
    }

    @Transactional(readOnly = true)
    public List<CourseMaterialResponse> getCourseMaterials(Long courseId) {
        return courseMaterialRepository.findByCourseId(courseId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMaterial(Long id) {
        if (!courseMaterialRepository.existsById(id)) {
            throw new ResourceNotFoundException("Course material", id);
        }
        courseMaterialRepository.deleteById(id);
    }

    private CourseMaterialResponse mapToResponse(CourseMaterial m) {
        return CourseMaterialResponse.builder()
                .id(m.getId())
                .courseId(m.getCourse().getId())
                .courseName(m.getCourse().getName())
                .title(m.getTitle())
                .description(m.getDescription())
                .url(m.getUrl())
                .type(m.getType())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
