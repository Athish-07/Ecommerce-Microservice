package com.example.api_gateway.config;

import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

@Configuration
public class CorrelationIdFilter {

    public static final String HEADER_NAME = "X-Correlation-ID";
    public static final String MDC_KEY = "correlationId";

    @Bean
    public GlobalFilter correlationIdGlobalFilter() {
        return (exchange, chain) -> {
            String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER_NAME);
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }

            var request = exchange.getRequest().mutate()
                    .header(HEADER_NAME, correlationId)
                    .build();
            exchange.getResponse().getHeaders().set(HEADER_NAME, correlationId);

            MDC.put(MDC_KEY, correlationId);
            return chain.filter(exchange.mutate().request(request).build())
                    .doFinally(signalType -> MDC.remove(MDC_KEY));
        };
    }
}
