package com.example.apartmentmanagerapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for ValidDayOfMonth annotation
 * Ensures day is between 1 and 31
 */
public class DayOfMonthValidator implements ConstraintValidator<ValidDayOfMonth, Integer> {
    
    @Override
    public void initialize(ValidDayOfMonth constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(Integer dayOfMonth, ConstraintValidatorContext context) {
        // Null values are valid (let @NotNull handle nullability)
        if (dayOfMonth == null) {
            return true;
        }
        
        // Check if day is between 1 and 31
        boolean isValid = dayOfMonth >= 1 && dayOfMonth <= 31;
        
        if (!isValid) {
            // Customize error message
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("Day of month must be between 1 and 31, but was %d", dayOfMonth)
            ).addConstraintViolation();
        }
        
        return isValid;
    }
}