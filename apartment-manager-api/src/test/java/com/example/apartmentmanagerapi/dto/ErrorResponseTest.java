package com.example.apartmentmanagerapi.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ErrorResponse DTO.
 * Tests the error response structure and builder functionality.
 */
@DisplayName("ErrorResponse DTO Tests")
class ErrorResponseTest {
    
    /**
     * Test creating ErrorResponse with all fields
     */
    @Test
    @DisplayName("Should create ErrorResponse with all fields using builder")
    void testErrorResponseBuilderAllFields() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        int status = 400;
        String error = "Bad Request";
        String message = "Validation failed";
        String path = "/api/users";
        String errorCode = "VALIDATION_ERROR";
        String correlationId = "123e4567-e89b-12d3-a456-426614174000";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("email", "Invalid email format");
        fieldErrors.put("username", "Username is required");
        String stackTrace = "java.lang.Exception: Test\n\tat com.example.Test.method(Test.java:10)";
        
        // When
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .errorCode(errorCode)
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .stackTrace(stackTrace)
                .build();
        
        // Then
        assertNotNull(errorResponse);
        assertEquals(timestamp, errorResponse.getTimestamp());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(error, errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(path, errorResponse.getPath());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(correlationId, errorResponse.getCorrelationId());
        assertEquals(fieldErrors, errorResponse.getFieldErrors());
        assertEquals(stackTrace, errorResponse.getStackTrace());
    }
    
    /**
     * Test creating ErrorResponse with minimal fields
     */
    @Test
    @DisplayName("Should create ErrorResponse with minimal fields")
    void testErrorResponseMinimalFields() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        int status = 404;
        String error = "Not Found";
        String message = "Resource not found";
        String path = "/api/users/123";
        
        // When
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
        
        // Then
        assertNotNull(errorResponse);
        assertEquals(timestamp, errorResponse.getTimestamp());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(error, errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(path, errorResponse.getPath());
        assertNull(errorResponse.getErrorCode());
        assertNull(errorResponse.getCorrelationId());
        assertNull(errorResponse.getFieldErrors());
        assertNull(errorResponse.getStackTrace());
    }
    
    /**
     * Test ErrorResponse builder with default values
     */
    @Test
    @DisplayName("Should create ErrorResponse with default values using builder")
    void testErrorResponseBuilderDefaults() {
        // When
        ErrorResponse errorResponse = ErrorResponse.builder().build();
        
        // Then
        assertNotNull(errorResponse);
        assertNull(errorResponse.getTimestamp());
        assertEquals(0, errorResponse.getStatus());
        assertNull(errorResponse.getError());
        assertNull(errorResponse.getMessage());
        assertNull(errorResponse.getPath());
        assertNull(errorResponse.getErrorCode());
        assertNull(errorResponse.getCorrelationId());
        assertNull(errorResponse.getFieldErrors());
        assertNull(errorResponse.getStackTrace());
    }
    
    /**
     * Test ErrorResponse static factory method
     */
    @Test
    @DisplayName("Should create ErrorResponse using static factory method")
    void testErrorResponseStaticFactory() {
        // Given
        int status = 404;
        String error = "Not Found";
        String message = "Resource not found";
        String path = "/api/users/123";
        
        // When
        ErrorResponse errorResponse = ErrorResponse.of(status, error, message, path);
        
        // Then
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getTimestamp());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(error, errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(path, errorResponse.getPath());
        assertNull(errorResponse.getErrorCode());
        assertNull(errorResponse.getCorrelationId());
        assertNull(errorResponse.getFieldErrors());
        assertNull(errorResponse.getStackTrace());
    }
    
    /**
     * Test ErrorResponse builder can be modified after creation
     */
    @Test
    @DisplayName("Should modify ErrorResponse after creation")
    void testErrorResponseModification() {
        // Given
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(200)
                .message("Initial message")
                .build();
        
        LocalDateTime timestamp = LocalDateTime.now();
        int status = 403;
        String error = "Forbidden";
        String message = "Access denied";
        String path = "/api/admin";
        String errorCode = "ACCESS_DENIED";
        String correlationId = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("role", "Insufficient privileges");
        
        // When
        errorResponse.setTimestamp(timestamp);
        errorResponse.setStatus(status);
        errorResponse.setError(error);
        errorResponse.setMessage(message);
        errorResponse.setPath(path);
        errorResponse.setErrorCode(errorCode);
        errorResponse.setCorrelationId(correlationId);
        errorResponse.setFieldErrors(fieldErrors);
        
        // Then
        assertEquals(timestamp, errorResponse.getTimestamp());
        assertEquals(status, errorResponse.getStatus());
        assertEquals(error, errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertEquals(path, errorResponse.getPath());
        assertEquals(errorCode, errorResponse.getErrorCode());
        assertEquals(correlationId, errorResponse.getCorrelationId());
        assertEquals(fieldErrors, errorResponse.getFieldErrors());
    }
    
    /**
     * Test ErrorResponse with empty field errors
     */
    @Test
    @DisplayName("Should handle empty field errors map")
    void testErrorResponseEmptyFieldErrors() {
        // Given
        Map<String, String> emptyFieldErrors = new HashMap<>();
        
        // When
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path("/api/test")
                .fieldErrors(emptyFieldErrors)
                .build();
        
        // Then
        assertNotNull(errorResponse.getFieldErrors());
        assertTrue(errorResponse.getFieldErrors().isEmpty());
    }
    
    /**
     * Test ErrorResponse equals and hashCode
     */
    @Test
    @DisplayName("Should correctly implement equals and hashCode")
    void testErrorResponseEqualsAndHashCode() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();
        String correlationId = "test-correlation-id";
        
        ErrorResponse errorResponse1 = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(404)
                .error("Not Found")
                .message("User not found")
                .path("/api/users/1")
                .errorCode("USER_NOT_FOUND")
                .correlationId(correlationId)
                .build();
        
        ErrorResponse errorResponse2 = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(404)
                .error("Not Found")
                .message("User not found")
                .path("/api/users/1")
                .errorCode("USER_NOT_FOUND")
                .correlationId(correlationId)
                .build();
        
        ErrorResponse errorResponse3 = ErrorResponse.builder()
                .timestamp(timestamp)
                .status(404)
                .error("Not Found")
                .message("Different message")
                .path("/api/users/1")
                .errorCode("USER_NOT_FOUND")
                .correlationId(correlationId)
                .build();
        
        // Then
        assertEquals(errorResponse1, errorResponse2);
        assertEquals(errorResponse1.hashCode(), errorResponse2.hashCode());
        assertNotEquals(errorResponse1, errorResponse3);
        assertNotEquals(errorResponse1.hashCode(), errorResponse3.hashCode());
        assertNotEquals(errorResponse1, null);
        assertNotEquals(errorResponse1, new Object());
    }
    
    /**
     * Test ErrorResponse toString
     */
    @Test
    @DisplayName("Should generate meaningful toString representation")
    void testErrorResponseToString() {
        // Given
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .status(400)
                .error("Bad Request")
                .message("Validation failed")
                .path("/api/users")
                .errorCode("VALIDATION_ERROR")
                .correlationId("test-id")
                .build();
        
        // When
        String toString = errorResponse.toString();
        
        // Then
        assertNotNull(toString);
        assertTrue(toString.contains("timestamp=2024-01-15T10:30"));
        assertTrue(toString.contains("status=400"));
        assertTrue(toString.contains("error=Bad Request"));
        assertTrue(toString.contains("message=Validation failed"));
        assertTrue(toString.contains("path=/api/users"));
        assertTrue(toString.contains("errorCode=VALIDATION_ERROR"));
        assertTrue(toString.contains("correlationId=test-id"));
    }
    
    /**
     * Test that ErrorResponse is properly annotated with Lombok
     */
    @Test
    @DisplayName("Should have proper Lombok annotations")
    void testLombokAnnotations() {
        // Given
        ErrorResponse errorResponse1 = ErrorResponse.builder().build();
        ErrorResponse errorResponse2 = ErrorResponse.builder().build();
        
        // Then - verify that Lombok generated methods exist and work
        assertNotNull(errorResponse1);
        assertNotNull(errorResponse2);
        assertDoesNotThrow(() -> errorResponse1.toString());
        assertDoesNotThrow(() -> errorResponse1.hashCode());
        assertDoesNotThrow(() -> errorResponse1.equals(errorResponse2));
    }
}