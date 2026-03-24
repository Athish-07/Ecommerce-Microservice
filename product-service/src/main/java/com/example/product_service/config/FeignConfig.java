package com.example.product_service.config;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@Configuration
public class FeignConfig {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";

    @Bean
    public RequestInterceptor authForwardInterceptor() {
        return requestTemplate -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            String correlationId = MDC.get("correlationId");

            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String tokenValue = jwtAuth.getToken().getTokenValue();
                requestTemplate.header("Authorization", "Bearer " + tokenValue);
            }

            if (correlationId != null && !correlationId.isBlank()) {
                requestTemplate.header(CORRELATION_ID_HEADER, correlationId);
            }
        };
    }
}
