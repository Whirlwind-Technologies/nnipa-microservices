package com.nnipa.tenant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.util.concurrent.Executor;

/**
 * Core application configuration for the Tenant Management Service.
 * Configures essential beans and settings for the application.
 */
@Slf4j
@Configuration
public class ApplicationConfig implements WebMvcConfigurer {

    /**
     * Configures CORS settings for the application.
     * In production, these should be more restrictive.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        log.info("CORS configuration applied for /api/** endpoints");
    }

    /**
     * Provides a Clock bean for consistent time handling across the application.
     * Using UTC for all time-related operations.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Configures ObjectMapper for JSON serialization/deserialization.
     * Sets up proper handling of Java 8 time types and other settings.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        log.info("ObjectMapper configured with JavaTimeModule and custom settings");
        return mapper;
    }

    /**
     * Configures RestTemplate for making HTTP calls to external services.
     * Can be enhanced with interceptors, error handlers, etc.
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Add interceptors, message converters, error handlers as needed
        log.info("RestTemplate bean configured");
        return restTemplate;
    }

    /**
     * Configures async task executor for handling asynchronous operations.
     * Used for background tasks like tenant provisioning.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("TenantAsync-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("Async TaskExecutor configured with core pool size: {}, max pool size: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        return executor;
    }
}