package com.nnipa.tenant;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Main application class for the Tenant Management Service.
 *
 * This service is responsible for managing multi-tenant operations including:
 * - Tenant lifecycle management (creation, suspension, deletion)
 * - Tenant provisioning and onboarding automation
 * - Tenant configuration and feature flag management
 * - Multi-tenant database schema management
 * - Tenant billing and subscription status
 */
@Slf4j
@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@EnableTransactionManagement
@ConfigurationPropertiesScan
public class TenantManagementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TenantManagementServiceApplication.class, args);
        log.info("🚀 Tenant Management Service started successfully!");
    }
}