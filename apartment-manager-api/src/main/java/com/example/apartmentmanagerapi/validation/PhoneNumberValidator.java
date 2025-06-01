package com.example.apartmentmanagerapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validator implementation for the {@link ValidPhoneNumber} annotation.
 * Validates phone numbers against common formats.
 */
public class PhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {
    
    /**
     * Regex pattern for validating phone numbers.
     * Supports various international and US formats.
     * Updated to support formats like Turkish phones: +90 542 604 53 06
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^(\\+\\d{1,3}[- ]?)?(\\(?\\d{1,4}\\)?[- ]?)(\\d{1,4}[- ]?)*(\\d{1,4})$"
    );
    
    /**
     * More restrictive pattern for stricter validation.
     * Ensures minimum length and proper formatting.
     */
    private static final Pattern STRICT_PHONE_PATTERN = Pattern.compile(
            "^(\\+\\d{1,3}[- ]?)?(\\(\\d{3}\\)|\\d{3})[- ]?\\d{3}[- ]?\\d{4}$"
    );
    
    private boolean allowEmpty;
    
    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Handle null values
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Handle empty values
        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            return allowEmpty;
        }
        
        // Remove common formatting characters for validation
        String cleanedNumber = trimmedValue.replaceAll("[\\s()-]", "");
        
        // Check if it contains only digits and optional + at the beginning
        if (!cleanedNumber.matches("^\\+?\\d+$")) {
            return false;
        }
        
        // Check minimum length (at least 7 digits for a valid phone number)
        int digitCount = cleanedNumber.replaceAll("\\+", "").length();
        if (digitCount < 7 || digitCount > 15) {
            return false;
        }
        
        // Validate against the pattern
        return PHONE_PATTERN.matcher(trimmedValue).matches();
    }
}