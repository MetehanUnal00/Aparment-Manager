package com.example.apartmentmanagerapi.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

/**
 * Test configuration for database-related settings.
 * Ensures proper H2 database configuration for tests.
 */
@TestConfiguration
@TestPropertySource(properties = {
    "spring.jpa.properties.hibernate.globally_quoted_identifiers=true",
    "spring.jpa.properties.hibernate.globally_quoted_identifiers_skip_column_definitions=true"
})
public class TestDatabaseConfig {
    
    /**
     * Bean to provide proper naming strategy for H2 database
     * This helps ensure compatibility between PostgreSQL and H2
     */
    @Bean
    public PhysicalNamingStrategy physicalNamingStrategy() {
        return new CamelCaseToUnderscoresNamingStrategy();
    }
}