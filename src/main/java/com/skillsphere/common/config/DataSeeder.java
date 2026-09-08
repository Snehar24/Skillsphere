package com.skillsphere.common.config;

import com.skillsphere.common.enums.Role;
import com.skillsphere.user.entity.User;
import com.skillsphere.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
    }

    private void seedAdminUser() {
        String adminEmail = "mohanraj.k1110@gmail.com";
        userRepository.findByEmail(adminEmail).ifPresentOrElse(
                user -> {
                    user.setName("System Administrator");
                    user.setPassword(passwordEncoder.encode("Mohan@2004"));
                    user.setRole(Role.ROLE_ADMIN);
                    user.setActive(true);
                    userRepository.save(user);
                    log.info("Admin user already exists and verified: {}", adminEmail);
                },
                () -> {
                    User admin = User.builder()
                            .name("System Administrator")
                            .email(adminEmail)
                            .password(passwordEncoder.encode("Mohan@2004"))
                            .phone("+919876543210")
                            .role(Role.ROLE_ADMIN)
                            .active(true)
                            .build();
                    userRepository.save(admin);
                    log.info("Admin user seeded successfully: {}", adminEmail);
                }
        );
    }
}
