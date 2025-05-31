package com.example.apartmentmanagerapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

/**
 * Configuration for JPA Auditing.
 * Enables automatic population of audit fields (created/updated by/at).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {
    
    /**
     * Bean that provides the current auditor (user) for audit fields.
     * Gets the username from the Spring Security context.
     * 
     * @return AuditorAware implementation
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }
    
    /**
     * Implementation of AuditorAware that gets the current user from Spring Security.
     */
    public static class SpringSecurityAuditorAware implements AuditorAware<String> {
        
        @Override
        public Optional<String> getCurrentAuditor() {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.of("system"); // Default for unauthenticated operations
            }
            
            // Handle anonymous users
            if (authentication.getPrincipal() instanceof String 
                && "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("anonymous");
            }
            
            // Get username from UserDetails
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                return Optional.of(userDetails.getUsername());
            }
            
            // Fallback to principal toString
            return Optional.of(authentication.getPrincipal().toString());
        }
    }
}