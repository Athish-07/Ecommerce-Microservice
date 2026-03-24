package com.example.auth_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.bootstrap.admin")
public record AdminBootstrapProperties(
        boolean enabled,
        String fullName,
        String email,
        String password
) {
}
