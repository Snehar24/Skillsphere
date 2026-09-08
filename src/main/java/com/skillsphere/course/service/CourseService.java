package com.skillsphere.course.service;

import com.skillsphere.course.dto.CourseRequest;
import com.skillsphere.course.dto.CourseResponse;
import com.skillsphere.course.entity.Course;
import com.skillsphere.course.repository.CourseRepository;
import com.skillsphere.exception.DuplicateResourceException;
import com.skillsphere.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(String search, Pageable pageable) {
        Page<Course> coursesPage;
        if (search != null && !search.trim().isEmpty()) {
            coursesPage = courseRepository.searchCourses(search, pageable);
        } else {
            coursesPage = courseRepository.findAll(pageable);
        }
        return coursesPage.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse createCourse(CourseRequest request) {
        if (courseRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Course name already exists: " + request.getName());
        }

        Course course = Course.builder()
                .name(request.getName())
                .description(request.getDescription())
                .duration(request.getDuration())
                .fee(request.getFee())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();
        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    @Transactional
    public CourseResponse updateCourse(Long id, CourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));

        if (!course.getName().equals(request.getName()) && courseRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Course name already exists: " + request.getName());
        }

        course.setName(request.getName());
        course.setDescription(request.getDescription());
        course.setDuration(request.getDuration());
        course.setFee(request.getFee());
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        course = courseRepository.save(course);
        return mapToResponse(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        courseRepository.delete(course);
    }

    private CourseResponse mapToResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .name(course.getName())
                .description(course.getDescription())
                .duration(course.getDuration())
                .fee(course.getFee())
                .status(course.getStatus())
                .build();
    }
}
