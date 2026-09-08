package com.skillsphere.certificate.repository;

import com.skillsphere.certificate.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByStudentId(Long studentId);
    Optional<Certificate> findByCertificateNumber(String certificateNumber);
    Optional<Certificate> findByVerificationCode(String verificationCode);
    boolean existsByCertificateNumber(String certificateNumber);
    boolean existsByVerificationCode(String verificationCode);
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
    Optional<Certificate> findByStudentIdAndCourseId(Long studentId, Long courseId);
}
