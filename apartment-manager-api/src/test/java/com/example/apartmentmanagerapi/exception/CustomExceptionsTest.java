package com.example.apartmentmanagerapi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for custom exception classes.
 * Tests all custom exceptions to ensure they properly handle messages, error codes, and field errors.
 */
@DisplayName("Custom Exceptions Tests")
class CustomExceptionsTest {
    
    /**
     * Test ResourceNotFoundException creation and properties
     */
    @Test
    @DisplayName("Should create ResourceNotFoundException with resource type and id")
    void testResourceNotFoundException() {
        // Given
        String resourceType = "User";
        Long resourceId = 123L;
        
        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);
        
        // Then
        assertEquals("User not found with id: 123", exception.getMessage());
        assertEquals(resourceType, exception.getResourceType());
        assertEquals(resourceId, exception.getResourceId());
        assertNotNull(exception.toString());
    }
    
    /**
     * Test ResourceNotFoundException with custom message
     */
    @Test
    @DisplayName("Should create ResourceNotFoundException with custom message")
    void testResourceNotFoundExceptionWithCustomMessage() {
        // Given
        String message = "Custom error message";
        String resourceType = "Building";
        Long resourceId = 456L;
        
        // When
        ResourceNotFoundException exception = new ResourceNotFoundException(message, resourceType, resourceId);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(resourceType, exception.getResourceType());
        assertEquals(resourceId, exception.getResourceId());
    }
    
    /**
     * Test ValidationException without field errors
     */
    @Test
    @DisplayName("Should create ValidationException without field errors")
    void testValidationExceptionWithoutFieldErrors() {
        // Given
        String message = "Validation failed";
        
        // When
        ValidationException exception = new ValidationException(message);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertFalse(exception.hasFieldErrors());
        assertTrue(exception.getFieldErrors().isEmpty());
    }
    
    /**
     * Test ValidationException with field errors
     */
    @Test
    @DisplayName("Should create ValidationException with field errors")
    void testValidationExceptionWithFieldErrors() {
        // Given
        String message = "Validation failed";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("email", "Invalid email format");
        fieldErrors.put("username", "Username is required");
        
        // When
        ValidationException exception = new ValidationException(message, fieldErrors);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertTrue(exception.hasFieldErrors());
        assertEquals(2, exception.getFieldErrors().size());
        assertEquals("Invalid email format", exception.getFieldErrors().get("email"));
    }
    
    /**
     * Test ValidationException with field-specific error
     */
    @Test
    @DisplayName("Should create ValidationException with field-specific error")
    void testValidationExceptionWithFieldSpecificError() {
        // Given
        String field = "email";
        String message = "Invalid email format";
        
        // When
        ValidationException exception = new ValidationException(field, message);
        
        // Then
        assertEquals("Validation failed for field 'email': Invalid email format", exception.getMessage());
        assertTrue(exception.hasFieldErrors());
        assertEquals(1, exception.getFieldErrors().size());
        assertEquals(message, exception.getFieldErrors().get(field));
    }
    
    /**
     * Test DuplicateResourceException
     */
    @Test
    @DisplayName("Should create DuplicateResourceException with resource info")
    void testDuplicateResourceException() {
        // Given
        String resourceType = "User";
        String fieldName = "email";
        Object fieldValue = "test@example.com";
        
        // When
        DuplicateResourceException exception = new DuplicateResourceException(resourceType, fieldName, fieldValue);
        
        // Then
        assertEquals("User already exists with email: test@example.com", exception.getMessage());
        assertEquals(resourceType, exception.getResourceType());
        assertEquals(fieldName, exception.getFieldName());
        assertEquals(fieldValue, exception.getFieldValue());
    }
    
    /**
     * Test UnauthorizedException
     */
    @Test
    @DisplayName("Should create UnauthorizedException with simple message")
    void testUnauthorizedException() {
        // Given
        String message = "User does not have permission to access this resource";
        
        // When
        UnauthorizedException exception = new UnauthorizedException(message);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("UnauthorizedException", exception.getErrorCode());
    }
    
    /**
     * Test BusinessRuleException
     */
    @Test
    @DisplayName("Should create BusinessRuleException with rule name")
    void testBusinessRuleException() {
        // Given
        String ruleName = "DeleteBuildingRule";
        String message = "Cannot delete building with active flats";
        
        // When
        BusinessRuleException exception = new BusinessRuleException(ruleName, message);
        
        // Then
        assertEquals("Cannot delete building with active flats", exception.getMessage());
        assertEquals(ruleName, exception.getRule());
        assertNull(exception.getContext());
    }
    
    /**
     * Test TechnicalException
     */
    @Test
    @DisplayName("Should create TechnicalException with component and operation")
    void testTechnicalException() {
        // Given
        String component = "Database";
        String operation = "save";
        String message = "Connection timeout";
        
        // When
        TechnicalException exception = new TechnicalException(component, operation, message);
        
        // Then
        assertEquals("Technical error in Database during save: Connection timeout", exception.getMessage());
        assertEquals(component, exception.getComponent());
        assertEquals(operation, exception.getOperation());
    }
    
    /**
     * Test TechnicalException with cause
     */
    @Test
    @DisplayName("Should create TechnicalException with message and cause")
    void testTechnicalExceptionWithCause() {
        // Given
        String message = "Failed to process request";
        Exception cause = new RuntimeException("Connection timeout");
        
        // When
        TechnicalException exception = new TechnicalException(message, cause);
        
        // Then
        assertEquals(message, exception.getMessage());
        assertEquals("TechnicalException", exception.getErrorCode());
        assertEquals(cause, exception.getCause());
        assertEquals("Connection timeout", exception.getCause().getMessage());
    }
    
    /**
     * Test exception inheritance hierarchy
     */
    @Test
    @DisplayName("Should verify all custom exceptions extend ApartmentManagerException")
    void testExceptionHierarchy() {
        // Given/When
        ResourceNotFoundException resourceNotFound = new ResourceNotFoundException("User", 1L);
        ValidationException validation = new ValidationException("test");
        DuplicateResourceException duplicate = new DuplicateResourceException("User", "email", "test@test.com");
        UnauthorizedException unauthorized = new UnauthorizedException("test");
        BusinessRuleException businessRule = new BusinessRuleException("rule", "test");
        TechnicalException technical = new TechnicalException("test", new RuntimeException("cause"));
        
        // Then
        assertTrue(resourceNotFound instanceof ApartmentManagerException);
        assertTrue(validation instanceof ApartmentManagerException);
        assertTrue(duplicate instanceof ApartmentManagerException);
        assertTrue(unauthorized instanceof ApartmentManagerException);
        assertTrue(businessRule instanceof ApartmentManagerException);
        assertTrue(technical instanceof ApartmentManagerException);
        
        // All should also be RuntimeExceptions
        assertTrue(resourceNotFound instanceof RuntimeException);
        assertTrue(validation instanceof RuntimeException);
        assertTrue(duplicate instanceof RuntimeException);
        assertTrue(unauthorized instanceof RuntimeException);
        assertTrue(businessRule instanceof RuntimeException);
        assertTrue(technical instanceof RuntimeException);
    }
}