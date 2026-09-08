package com.skillsphere.auth.repository;

import com.skillsphere.auth.entity.PasswordResetToken;
import com.skillsphere.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUserAndOtpAndUsedFalseAndExpiryTimeAfter(User user, String otp, LocalDateTime now);

    Optional<PasswordResetToken> findFirstByUserAndUsedFalseOrderByCreatedAtDesc(User user);

    void deleteByUser(User user);
}
