package com.example.api_gateway.config;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpCookie;

@Configuration
public class AuthCookieRelayFilter {

    private static final String AUTH_COOKIE_NAME = "NOVACART_AUTH";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Bean
    public GlobalFilter authCookieRelayGlobalFilter() {
        return (exchange, chain) -> {
            String existingAuthorization = exchange.getRequest().getHeaders().getFirst(AUTHORIZATION_HEADER);
            if (existingAuthorization != null && !existingAuthorization.isBlank()) {
                return chain.filter(exchange);
            }

            HttpCookie authCookie = exchange.getRequest().getCookies().getFirst(AUTH_COOKIE_NAME);
            if (authCookie == null || authCookie.getValue() == null || authCookie.getValue().isBlank()) {
                return chain.filter(exchange);
            }

            var request = exchange.getRequest().mutate()
                    .header(AUTHORIZATION_HEADER, "Bearer " + authCookie.getValue())
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        };
    }
}
