package com.example.apartmentmanagerapi.dto.validation;

import com.example.apartmentmanagerapi.dto.MonthlyDueRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;

/**
 * Validator for MonthlyDueRequest to ensure proper amount fields based on generation mode.
 * 
 * Implements the following validation rules:
 * 1. Uniform mode (useFlatsMonthlyRent=false): dueAmount must be present and positive
 * 2. Rent-based mode (useFlatsMonthlyRent=true): At least one fallback (fallbackAmount or dueAmount) must be present and positive
 */
public class MonthlyDueGenerationValidator 
        implements ConstraintValidator<ValidMonthlyDueGeneration, MonthlyDueRequest> {
    
    @Override
    public void initialize(ValidMonthlyDueGeneration constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(MonthlyDueRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Let @NotNull handle null objects
        }
        
        // Clear default message and prepare for custom messages
        context.disableDefaultConstraintViolation();
        
        // Check generation mode
        boolean useFlatsMonthlyRent = Boolean.TRUE.equals(request.getUseFlatsMonthlyRent());
        
        if (useFlatsMonthlyRent) {
            // Rent-based generation mode
            // At least one fallback must be available
            boolean hasFallbackAmount = isValidAmount(request.getFallbackAmount());
            boolean hasDueAmount = isValidAmount(request.getDueAmount());
            
            if (!hasFallbackAmount && !hasDueAmount) {
                context.buildConstraintViolationWithTemplate(
                    "When using flat monthly rents, either fallbackAmount or dueAmount " +
                    "must be provided as a fallback for flats without rent"
                ).addConstraintViolation();
                return false;
            }
            
            // Log warning if only ultimate fallback is available
            if (!hasFallbackAmount && hasDueAmount) {
                // This is valid but might be worth noting
                // The service layer can log this scenario
            }
            
        } else {
            // Uniform generation mode (default)
            // dueAmount is required
            if (!isValidAmount(request.getDueAmount())) {
                context.buildConstraintViolationWithTemplate(
                    "Due amount is required and must be greater than zero when not using flat monthly rents"
                ).addConstraintViolation();
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if the given amount is valid (not null and positive).
     * 
     * @param amount The amount to validate
     * @return true if the amount is valid, false otherwise
     */
    private boolean isValidAmount(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }
}