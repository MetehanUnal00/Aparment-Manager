package com.example.apartmentmanagerapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;

/**
 * Validator implementation for the {@link ValidDateRange} annotation.
 * Validates that a start date is before or equal to an end date.
 * Supports both LocalDate and LocalDateTime fields.
 */
@Slf4j
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, Object> {
    
    private String startDateFieldName;
    private String endDateFieldName;
    
    @Override
    public void initialize(ValidDateRange constraintAnnotation) {
        this.startDateFieldName = constraintAnnotation.startDate();
        this.endDateFieldName = constraintAnnotation.endDate();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        try {
            // Get the field values using reflection
            Field startDateField = value.getClass().getDeclaredField(startDateFieldName);
            Field endDateField = value.getClass().getDeclaredField(endDateFieldName);
            
            startDateField.setAccessible(true);
            endDateField.setAccessible(true);
            
            Object startDateValue = startDateField.get(value);
            Object endDateValue = endDateField.get(value);
            
            // If either date is null, consider it valid (let other validators handle null checks)
            if (startDateValue == null || endDateValue == null) {
                return true;
            }
            
            // Validate based on the type of date objects
            if (startDateValue instanceof LocalDate && endDateValue instanceof LocalDate) {
                LocalDate startDate = (LocalDate) startDateValue;
                LocalDate endDate = (LocalDate) endDateValue;
                return !startDate.isAfter(endDate);
            } else if (startDateValue instanceof LocalDateTime && endDateValue instanceof LocalDateTime) {
                LocalDateTime startDateTime = (LocalDateTime) startDateValue;
                LocalDateTime endDateTime = (LocalDateTime) endDateValue;
                return !startDateTime.isAfter(endDateTime);
            } else if (startDateValue instanceof Temporal && endDateValue instanceof Temporal) {
                // Generic temporal comparison for other date/time types
                log.warn("Using generic temporal comparison for types: {} and {}", 
                        startDateValue.getClass().getSimpleName(), 
                        endDateValue.getClass().getSimpleName());
                return true; // Allow for now, specific implementations can be added later
            } else {
                log.error("Invalid date field types for validation: {} and {}", 
                        startDateValue.getClass().getSimpleName(), 
                        endDateValue.getClass().getSimpleName());
                return false;
            }
            
        } catch (NoSuchFieldException e) {
            log.error("Field not found during date range validation: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid field names specified in @ValidDateRange", e);
        } catch (IllegalAccessException e) {
            log.error("Cannot access field during date range validation: {}", e.getMessage());
            throw new RuntimeException("Cannot access field for validation", e);
        }
    }
}