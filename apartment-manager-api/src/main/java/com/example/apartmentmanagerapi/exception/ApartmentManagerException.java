package com.example.apartmentmanagerapi.exception;

/**
 * Base exception class for all apartment manager business exceptions.
 * This provides a common ancestor for all custom exceptions in the application.
 */
public abstract class ApartmentManagerException extends RuntimeException {
    
    /**
     * Error code for categorizing exceptions
     */
    private final String errorCode;
    
    /**
     * Constructor with message only
     * @param message The error message
     */
    protected ApartmentManagerException(String message) {
        super(message);
        this.errorCode = this.getClass().getSimpleName();
    }
    
    /**
     * Constructor with message and error code
     * @param message The error message
     * @param errorCode Custom error code
     */
    protected ApartmentManagerException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    /**
     * Constructor with message and cause
     * @param message The error message
     * @param cause The underlying cause
     */
    protected ApartmentManagerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = this.getClass().getSimpleName();
    }
    
    /**
     * Constructor with message, error code, and cause
     * @param message The error message
     * @param errorCode Custom error code
     * @param cause The underlying cause
     */
    protected ApartmentManagerException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Get the error code associated with this exception
     * @return The error code
     */
    public String getErrorCode() {
        return errorCode;
    }
}