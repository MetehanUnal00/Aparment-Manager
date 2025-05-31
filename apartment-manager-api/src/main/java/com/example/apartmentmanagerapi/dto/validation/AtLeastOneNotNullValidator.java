package com.example.apartmentmanagerapi.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;

/**
 * Validator implementation for AtLeastOneNotNull annotation.
 * Checks that at least one of the specified fields in the annotated object is not null.
 */
public class AtLeastOneNotNullValidator implements ConstraintValidator<AtLeastOneNotNull, Object> {
    
    private String[] fieldNames;
    
    @Override
    public void initialize(AtLeastOneNotNull constraintAnnotation) {
        this.fieldNames = constraintAnnotation.fields();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // If the object itself is null, consider it valid (let @NotNull handle that)
        if (value == null) {
            return true;
        }
        
        // Check each specified field
        for (String fieldName : fieldNames) {
            try {
                // Get the field from the object's class
                Field field = value.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                
                // Get the field value
                Object fieldValue = field.get(value);
                
                // If any field is not null, validation passes
                if (fieldValue != null) {
                    return true;
                }
            } catch (NoSuchFieldException | IllegalAccessException e) {
                // Log error but don't fail validation
                // In production, you might want to log this properly
                throw new RuntimeException("Error accessing field: " + fieldName, e);
            }
        }
        
        // If we get here, all fields were null
        return false;
    }
}