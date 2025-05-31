package com.example.apartmentmanagerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure that a date is today or in the future.
 * Useful for validating due dates, scheduled dates, etc.
 * 
 * Example usage:
 * <pre>
 * {@code
 * public class MonthlyDueRequest {
 *     @FutureOrToday(message = "Due date must be today or in the future")
 *     private LocalDate dueDate;
 *     // getters and setters
 * }
 * }
 * </pre>
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FutureOrTodayValidator.class)
@Documented
public @interface FutureOrToday {
    
    /**
     * The error message to display when validation fails.
     * 
     * @return the error message
     */
    String message() default "Date must be today or in the future";
    
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
}