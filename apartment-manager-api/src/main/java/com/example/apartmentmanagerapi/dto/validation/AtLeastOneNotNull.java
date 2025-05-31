package com.example.apartmentmanagerapi.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom validation annotation to ensure at least one of the specified fields is not null.
 * Used for validating mutually exclusive or at-least-one-required field scenarios.
 */
@Documented
@Constraint(validatedBy = AtLeastOneNotNullValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AtLeastOneNotNull {
    /**
     * Error message to display when validation fails
     */
    String message() default "At least one of the specified fields must be provided";
    
    /**
     * Validation groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Payload for clients
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Field names to check
     * At least one of these fields must be non-null
     */
    String[] fields();
}