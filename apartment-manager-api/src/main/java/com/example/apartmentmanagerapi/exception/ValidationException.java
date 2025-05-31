package com.example.apartmentmanagerapi.exception;

import java.util.HashMap;
import java.util.Map;

/**
 * Exception thrown when validation fails for a request.
 * This typically maps to HTTP 400 status code.
 */
public class ValidationException extends ApartmentManagerException {
    
    /**
     * Map of field names to error messages for field-specific validation errors
     */
    private final Map<String, String> fieldErrors;
    
    /**
     * Constructor for general validation error
     * @param message The validation error message
     */
    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }
    
    /**
     * Constructor for field-specific validation error
     * @param field The field that failed validation
     * @param message The validation error message
     */
    public ValidationException(String field, String message) {
        super(String.format("Validation failed for field '%s': %s", field, message));
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, message);
    }
    
    /**
     * Constructor for multiple field validation errors
     * @param fieldErrors Map of field names to error messages
     */
    public ValidationException(Map<String, String> fieldErrors) {
        super("Validation failed for multiple fields");
        this.fieldErrors = new HashMap<>(fieldErrors);
    }
    
    /**
     * Constructor with custom message and field errors
     * @param message The overall validation error message
     * @param fieldErrors Map of field names to error messages
     */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message);
        this.fieldErrors = new HashMap<>(fieldErrors);
    }
    
    /**
     * Add a field-specific error
     * @param field The field name
     * @param errorMessage The error message for the field
     */
    public void addFieldError(String field, String errorMessage) {
        this.fieldErrors.put(field, errorMessage);
    }
    
    /**
     * Get all field errors
     * @return Map of field names to error messages
     */
    public Map<String, String> getFieldErrors() {
        return new HashMap<>(fieldErrors);
    }
    
    /**
     * Check if there are field-specific errors
     * @return true if there are field errors, false otherwise
     */
    public boolean hasFieldErrors() {
        return !fieldErrors.isEmpty();
    }
}