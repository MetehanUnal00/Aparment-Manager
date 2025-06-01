package com.example.apartmentmanagerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validation annotation to ensure day of month is valid (1-31)
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DayOfMonthValidator.class)
@Documented
public @interface ValidDayOfMonth {
    
    String message() default "Day of month must be between 1 and 31";
    
    Class<?>[] groups() default {};
    
    Class<? extends Payload>[] payload() default {};
}