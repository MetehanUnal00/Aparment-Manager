package com.example.apartmentmanagerapi.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Validates that MonthlyDueRequest has appropriate amount fields based on generation mode.
 * 
 * Rules:
 * - When useFlatsMonthlyRent=false (uniform mode): dueAmount must be present and positive
 * - When useFlatsMonthlyRent=true (rent-based mode): Either fallbackAmount or dueAmount must be present and positive
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MonthlyDueGenerationValidator.class)
@Documented
public @interface ValidMonthlyDueGeneration {
    String message() default "Invalid monthly due generation request";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}