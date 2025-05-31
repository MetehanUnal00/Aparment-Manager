package com.example.apartmentmanagerapi.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;

/**
 * Validator implementation for the {@link FutureOrToday} annotation.
 * Validates that a date is today or in the future.
 * Supports LocalDate, LocalDateTime, and ZonedDateTime.
 */
public class FutureOrTodayValidator implements ConstraintValidator<FutureOrToday, Temporal> {
    
    @Override
    public void initialize(FutureOrToday constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(Temporal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Handle different temporal types
        if (value instanceof LocalDate) {
            LocalDate date = (LocalDate) value;
            return !date.isBefore(LocalDate.now());
        } else if (value instanceof LocalDateTime) {
            LocalDateTime dateTime = (LocalDateTime) value;
            // For LocalDateTime, we compare dates only (ignore time component for "today" comparison)
            return !dateTime.toLocalDate().isBefore(LocalDate.now());
        } else if (value instanceof ZonedDateTime) {
            ZonedDateTime zonedDateTime = (ZonedDateTime) value;
            // Convert to local date in the same timezone for comparison
            return !zonedDateTime.toLocalDate().isBefore(LocalDate.now());
        } else {
            // For other temporal types, try to extract the date component
            if (value.isSupported(ChronoField.EPOCH_DAY)) {
                long epochDay = value.getLong(ChronoField.EPOCH_DAY);
                long todayEpochDay = LocalDate.now().toEpochDay();
                return epochDay >= todayEpochDay;
            }
        }
        
        // If we can't determine the date, consider it valid
        return true;
    }
}