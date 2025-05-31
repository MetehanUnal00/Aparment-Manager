package com.example.apartmentmanagerapi.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response structure for API errors.
 * Provides consistent error information to clients.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code
     */
    private int status;
    
    /**
     * HTTP status reason phrase (e.g., "Not Found", "Bad Request")
     */
    private String error;
    
    /**
     * Human-readable error message
     */
    private String message;
    
    /**
     * The API path where the error occurred
     */
    private String path;
    
    /**
     * Error code for categorizing the error (e.g., "RESOURCE_NOT_FOUND")
     */
    private String errorCode;
    
    /**
     * Correlation ID for tracking the request across systems
     */
    private String correlationId;
    
    /**
     * Field-specific validation errors (field name -> error message)
     * Only included for validation errors
     */
    private Map<String, String> fieldErrors;
    
    /**
     * Additional details about the error
     * Only included in development/debug mode
     */
    private Object details;
    
    /**
     * Stack trace information
     * Only included in development mode, never in production
     */
    private String stackTrace;
    
    /**
     * Helper method to create an error response for common cases
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}