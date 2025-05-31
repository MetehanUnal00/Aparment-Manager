package com.example.apartmentmanagerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation for phone number validation.
 * Validates phone numbers to ensure they match common formats.
 * 
 * Supported formats:
 * - International: +1234567890, +1-234-567-890
 * - US: (123) 456-7890, 123-456-7890, 1234567890
 * - General: Numbers with optional country code, spaces, hyphens, parentheses
 * 
 * Example usage:
 * <pre>
 * {@code
 * public class TenantRequest {
 *     @ValidPhoneNumber
 *     private String phoneNumber;
 *     // getters and setters
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
@Documented
public @interface ValidPhoneNumber {
    
    /**
     * The error message to display when validation fails.
     * 
     * @return the error message
     */
    String message() default "Invalid phone number format";
    
    /**
     * The validation groups this constraint belongs to.
     * 
     * @return the groups
     */
    Class<?>[] groups() default {};
    
    /**
     * Additional payload information about the validation error.
     * 
     * @return the payload
     */
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Whether to allow empty/blank phone numbers.
     * Default is true (empty values are considered valid).
     * 
     * @return true if empty values are allowed
     */
    boolean allowEmpty() default true;
}