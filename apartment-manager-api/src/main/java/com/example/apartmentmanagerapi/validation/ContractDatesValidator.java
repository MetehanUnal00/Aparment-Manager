package com.example.apartmentmanagerapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.time.LocalDate;

/**
 * Validator for ValidContractDates annotation
 * Ensures contract end date is after start date
 */
@Slf4j
public class ContractDatesValidator implements ConstraintValidator<ValidContractDates, Object> {
    
    private String startDateField;
    private String endDateField;
    
    @Override
    public void initialize(ValidContractDates constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDateField();
        this.endDateField = constraintAnnotation.endDateField();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        
        try {
            Field startField = value.getClass().getDeclaredField(startDateField);
            Field endField = value.getClass().getDeclaredField(endDateField);
            
            startField.setAccessible(true);
            endField.setAccessible(true);
            
            LocalDate startDate = (LocalDate) startField.get(value);
            LocalDate endDate = (LocalDate) endField.get(value);
            
            // If either date is null, skip validation (let @NotNull handle it)
            if (startDate == null || endDate == null) {
                return true;
            }
            
            // End date must be after start date
            boolean isValid = endDate.isAfter(startDate);
            
            if (!isValid) {
                // Customize error message with actual dates
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    String.format("Contract end date (%s) must be after start date (%s)", 
                                endDate, startDate)
                ).addConstraintViolation();
            }
            
            return isValid;
            
        } catch (Exception e) {
            log.error("Error validating contract dates", e);
            return false;
        }
    }
}