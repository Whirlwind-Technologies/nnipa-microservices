package com.nnipa.tenant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Tenant Management Service.
 * Configures API documentation with proper context path and security.
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api/v1}")
    private String contextPath;

    @Value("${server.port:8081}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("Tenant Management Service API")
                .description("Multi-tenant management service for the Nnipa platform. " +
                        "Provides comprehensive tenant lifecycle management, subscription handling, " +
                        "feature flags, and usage tracking.")
                .version("1.0.0")
                .contact(new Contact()
                        .name("Nnipa Platform Team")
                        .email("support@nnipa.com")
                        .url("https://nnipa.com"))
                .license(new License()
                        .name("Proprietary")
                        .url("https://nnipa.com/license"));
    }

    private List<Server> getServers() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort + contextPath);
        localServer.setDescription("Local Development Server");

        Server devServer = new Server();
        devServer.setUrl("https://dev.nnipa.com" + contextPath);
        devServer.setDescription("Development Server");

        Server prodServer = new Server();
        prodServer.setUrl("https://api.nnipa.com" + contextPath);
        prodServer.setDescription("Production Server");

        return List.of(localServer, devServer, prodServer);
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("JWT Bearer token authentication. " +
                        "Enter the token without the 'Bearer' prefix.");
    }
}