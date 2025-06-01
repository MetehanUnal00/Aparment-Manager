package com.example.apartmentmanagerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure contract dates are logical
 * Validates that end date is after start date
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ContractDatesValidator.class)
@Documented
public @interface ValidContractDates {
    
    String message() default "Contract end date must be after start date";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
    
    /**
     * Field name for start date
     */
    String startDateField() default "startDate";
    
    /**
     * Field name for end date
     */
    String endDateField() default "endDate";
}