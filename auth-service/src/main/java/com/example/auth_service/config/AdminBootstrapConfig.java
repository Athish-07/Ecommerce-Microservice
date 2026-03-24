package com.example.auth_service.config;

import com.example.auth_service.entity.BootstrapState;
import com.example.auth_service.entity.Role;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.BootstrapStateRepository;
import com.example.auth_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapConfig {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapConfig.class);
    private static final String ADMIN_BOOTSTRAP_KEY = "ADMIN_BOOTSTRAP_COMPLETED";

    @Bean
    public ApplicationRunner adminBootstrapRunner(
            AdminBootstrapProperties properties,
            UserRepository userRepository,
            BootstrapStateRepository bootstrapStateRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            if (!properties.enabled()) {
                return;
            }

            if (!StringUtils.hasText(properties.fullName())
                    || !StringUtils.hasText(properties.email())
                    || !StringUtils.hasText(properties.password())) {
                throw new IllegalStateException("Admin bootstrap is enabled but fullName, email, or password is missing");
            }

            if (bootstrapStateRepository.existsByBootstrapKey(ADMIN_BOOTSTRAP_KEY)) {
                log.info("Admin bootstrap skipped because completion marker already exists");
                return;
            }

            if (userRepository.existsByEmail(properties.email())) {
                bootstrapStateRepository.save(
                        BootstrapState.builder()
                                .bootstrapKey(ADMIN_BOOTSTRAP_KEY)
                                .completedAt(LocalDateTime.now())
                                .build()
                );
                log.info("Admin bootstrap marked complete because user already exists for {}", properties.email());
                return;
            }

            User admin = User.builder()
                    .fullName(properties.fullName())
                    .email(properties.email())
                    .password(passwordEncoder.encode(properties.password()))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
            bootstrapStateRepository.save(
                    BootstrapState.builder()
                            .bootstrapKey(ADMIN_BOOTSTRAP_KEY)
                            .completedAt(LocalDateTime.now())
                            .build()
            );
            log.info("Seeded bootstrap admin user for {}", properties.email());
        };
    }
}
