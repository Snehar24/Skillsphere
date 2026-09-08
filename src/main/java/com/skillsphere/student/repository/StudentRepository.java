package com.skillsphere.student.repository;

import com.skillsphere.student.entity.Student;
import com.skillsphere.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Student> findByUser(User user);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Student> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"user"})
    Optional<Student> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);

    @Query("SELECT s FROM Student s JOIN s.user u WHERE " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(s.registrationNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> searchStudents(@Param("search") String search, Pageable pageable);
}
