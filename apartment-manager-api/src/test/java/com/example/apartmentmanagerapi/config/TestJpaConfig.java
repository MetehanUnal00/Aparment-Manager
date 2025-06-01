package com.example.apartmentmanagerapi.config;

import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.Map;

/**
 * Test JPA configuration to ensure H2 compatibility
 * Forces Hibernate to not use RETURNING clause which H2 doesn't support
 */
@TestConfiguration
public class TestJpaConfig {
    
    /**
     * Customize Hibernate properties for H2 compatibility
     * This customizer ensures our H2DialectCustom is properly applied
     * and overrides any conflicting settings
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (hibernateProperties) -> {
            // Force our custom H2 dialect - using both property names for compatibility
            hibernateProperties.put("hibernate.dialect", "com.example.apartmentmanagerapi.config.H2DialectCustom");
            hibernateProperties.put("spring.jpa.database-platform", "com.example.apartmentmanagerapi.config.H2DialectCustom");
            
            // Disable all features that use RETURNING clause
            hibernateProperties.put("hibernate.jdbc.use_get_generated_keys", "true"); // Changed to true to use getGeneratedKeys
            hibernateProperties.put("hibernate.id.new_generator_mappings", "true"); // Changed to true for better ID generation
            hibernateProperties.put("hibernate.id.disable_delayed_identity_inserts", "true");
            hibernateProperties.put("hibernate.jdbc.use_streams_for_binary", "false");
            hibernateProperties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
            
            // H2 specific identity column settings
            hibernateProperties.put("hibernate.dialect.h2.use_sequences_for_identity_columns", "false");
            hibernateProperties.put("hibernate.id.optimizer.pooled.prefer_lo", "false");
            
            // Additional H2 specific settings
            hibernateProperties.put("hibernate.globally_quoted_identifiers", "true");
            hibernateProperties.put("hibernate.globally_quoted_identifiers_skip_column_definitions", "true");
            
            // Force the use of SCOPE_IDENTITY() instead of IDENTITY()
            hibernateProperties.put("hibernate.jdbc.batch_size", "0"); // Disable batching to avoid identity issues
        };
    }
}