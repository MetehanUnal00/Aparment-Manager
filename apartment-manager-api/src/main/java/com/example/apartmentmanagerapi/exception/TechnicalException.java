package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a technical error occurs (database, external service, etc.).
 * This typically maps to HTTP 500 Internal Server Error status code.
 */
public class TechnicalException extends ApartmentManagerException {
    
    /**
     * The component or service where the error occurred
     */
    private final String component;
    
    /**
     * The operation that failed
     */
    private final String operation;
    
    /**
     * Constructor for simple technical error
     * @param message The error message
     * @param cause The underlying cause
     */
    public TechnicalException(String message, Throwable cause) {
        super(message, cause);
        this.component = null;
        this.operation = null;
    }
    
    /**
     * Constructor with component information
     * @param component The component where the error occurred (e.g., "Database", "EmailService")
     * @param operation The operation that failed (e.g., "save", "sendEmail")
     * @param message The error message
     */
    public TechnicalException(String component, String operation, String message) {
        super(String.format("Technical error in %s during %s: %s", component, operation, message));
        this.component = component;
        this.operation = operation;
    }
    
    /**
     * Constructor with component information and cause
     * @param component The component where the error occurred
     * @param operation The operation that failed
     * @param message The error message
     * @param cause The underlying cause
     */
    public TechnicalException(String component, String operation, String message, Throwable cause) {
        super(String.format("Technical error in %s during %s: %s", component, operation, message), cause);
        this.component = component;
        this.operation = operation;
    }
    
    /**
     * Get the component where the error occurred
     * @return The component name
     */
    public String getComponent() {
        return component;
    }
    
    /**
     * Get the operation that failed
     * @return The operation name
     */
    public String getOperation() {
        return operation;
    }
}