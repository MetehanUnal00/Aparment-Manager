package com.example.apartmentmanagerapi.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Custom validation annotation to ensure that a date range is valid.
 * Validates that the start date is before or equal to the end date.
 * Apply this annotation at the class level.
 * 
 * Example usage:
 * <pre>
 * {@code
 * @ValidDateRange(startDate = "startDate", endDate = "endDate")
 * public class DateRangeRequest {
 *     private LocalDate startDate;
 *     private LocalDate endDate;
 *     // getters and setters
 * }
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
@Documented
public @interface ValidDateRange {
    
    /**
     * The error message to display when validation fails.
     * 
     * @return the error message
     */
    String message() default "Start date must be before or equal to end date";
    
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
     * The name of the field containing the start date.
     * 
     * @return the start date field name
     */
    String startDate();
    
    /**
     * The name of the field containing the end date.
     * 
     * @return the end date field name
     */
    String endDate();
}