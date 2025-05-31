package com.example.apartmentmanagerapi.config;

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
 * OpenAPI configuration for generating API documentation.
 * Configures Swagger UI with JWT authentication support.
 * Provides comprehensive API metadata and security schemes.
 */
@Configuration
public class OpenApiConfig {

    @Value("${application.version:0.0.1-SNAPSHOT}")
    private String appVersion;

    /**
     * Configures OpenAPI specification with API metadata and security schemes.
     * Sets up JWT Bearer token authentication for protected endpoints.
     * 
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Define the security scheme name
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                // API metadata information
                .info(new Info()
                        .title("Apartment Manager API")
                        .version(appVersion)
                        .description("RESTful API for managing apartment buildings, flats, payments, and expenses. " +
                                "Provides comprehensive functionality for property management including tenant tracking, " +
                                "monthly due generation, payment recording, and expense management.")
                        .contact(new Contact()
                                .name("Apartment Manager Support")
                                .email("support@apartmentmanager.com")
                                .url("https://apartmentmanager.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                
                // Server configurations
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("https://api.apartmentmanager.com")
                                .description("Production server")))
                
                // Security requirement - all endpoints require JWT unless specified otherwise
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName))
                
                // Security scheme configuration for JWT
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT token authentication. " +
                                                "Obtain the token from /api/auth/login endpoint. " +
                                                "Format: Bearer {token}")));
    }
}