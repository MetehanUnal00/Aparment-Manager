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
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return (hibernateProperties) -> {
            // Force H2 dialect
            hibernateProperties.put("hibernate.dialect", "com.example.apartmentmanagerapi.config.H2DialectCustom");
            
            // Disable all features that use RETURNING clause
            hibernateProperties.put("hibernate.jdbc.use_get_generated_keys", "false");
            hibernateProperties.put("hibernate.id.new_generator_mappings", "false");
            hibernateProperties.put("hibernate.id.disable_delayed_identity_inserts", "true");
            hibernateProperties.put("hibernate.jdbc.use_streams_for_binary", "false");
            hibernateProperties.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
            
            // Additional H2 specific settings
            hibernateProperties.put("hibernate.globally_quoted_identifiers", "true");
            hibernateProperties.put("hibernate.globally_quoted_identifiers_skip_column_definitions", "true");
        };
    }
}