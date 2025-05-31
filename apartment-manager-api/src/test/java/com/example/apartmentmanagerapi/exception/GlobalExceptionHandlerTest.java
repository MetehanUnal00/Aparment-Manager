package com.example.apartmentmanagerapi.exception;

import com.example.apartmentmanagerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GlobalExceptionHandler.
 * Tests all exception handling methods and response formatting.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {
    
    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;
    
    @Mock
    private HttpServletRequest request;
    
    private static final String REQUEST_URI = "/api/test";
    
    @BeforeEach
    void setUp() {
        // Configure common mock behavior
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        
        // Set includeStackTrace to false for testing
        ReflectionTestUtils.setField(globalExceptionHandler, "includeStackTrace", false);
    }
    
    /**
     * Test handling of ResourceNotFoundException
     */
    @Test
    @DisplayName("Should handle ResourceNotFoundException and return 404")
    void testHandleResourceNotFoundException() {
        // Given
        String resourceType = "User";
        Long resourceId = 123L;
        ResourceNotFoundException exception = new ResourceNotFoundException(resourceType, resourceId);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(404, errorResponse.getStatus());
        assertEquals("Not Found", errorResponse.getError());
        assertEquals("User not found with id: 123", errorResponse.getMessage());
        assertEquals(REQUEST_URI, errorResponse.getPath());
        assertEquals("ResourceNotFoundException", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getCorrelationId());
        assertNotNull(errorResponse.getTimestamp());
        assertNull(errorResponse.getFieldErrors());
    }
    
    /**
     * Test handling of ValidationException without field errors
     */
    @Test
    @DisplayName("Should handle ValidationException without field errors and return 400")
    void testHandleValidationExceptionWithoutFieldErrors() {
        // Given
        String message = "Validation failed";
        ValidationException exception = new ValidationException(message);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertNull(errorResponse.getFieldErrors());
    }
    
    /**
     * Test handling of ValidationException with field errors
     */
    @Test
    @DisplayName("Should handle ValidationException with field errors and return 400")
    void testHandleValidationExceptionWithFieldErrors() {
        // Given
        String message = "Validation failed";
        Map<String, String> fieldErrors = new HashMap<>();
        fieldErrors.put("email", "Invalid email format");
        fieldErrors.put("username", "Username is required");
        ValidationException exception = new ValidationException(message, fieldErrors);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleValidationException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(fieldErrors, errorResponse.getFieldErrors());
        assertEquals(2, errorResponse.getFieldErrors().size());
    }
    
    /**
     * Test handling of DuplicateResourceException
     */
    @Test
    @DisplayName("Should handle DuplicateResourceException and return 409")
    void testHandleDuplicateResourceException() {
        // Given
        String resourceType = "User";
        String fieldName = "email";
        String fieldValue = "test@example.com";
        DuplicateResourceException exception = new DuplicateResourceException(resourceType, fieldName, fieldValue);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDuplicateResourceException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(409, errorResponse.getStatus());
        assertEquals("Conflict", errorResponse.getError());
        assertEquals("User already exists with email: test@example.com", errorResponse.getMessage());
        assertEquals("DuplicateResourceException", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of UnauthorizedException
     */
    @Test
    @DisplayName("Should handle UnauthorizedException and return 403")
    void testHandleUnauthorizedException() {
        // Given
        String message = "Insufficient permissions";
        UnauthorizedException exception = new UnauthorizedException(message);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleUnauthorizedException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Forbidden", errorResponse.getError());
        assertEquals("UnauthorizedException", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of BusinessRuleException
     */
    @Test
    @DisplayName("Should handle BusinessRuleException and return 422")
    void testHandleBusinessRuleException() {
        // Given
        String message = "Cannot delete building with active flats";
        BusinessRuleException exception = new BusinessRuleException(message);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleBusinessRuleException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(422, errorResponse.getStatus());
        assertEquals("Unprocessable Entity", errorResponse.getError());
        assertEquals(message, errorResponse.getMessage());
        assertEquals("BusinessRuleException", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of TechnicalException
     */
    @Test
    @DisplayName("Should handle TechnicalException and return 500")
    void testHandleTechnicalException() {
        // Given
        String message = "Database connection failed";
        Exception cause = new RuntimeException("Connection timeout");
        TechnicalException exception = new TechnicalException(message, cause);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTechnicalException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("Internal Server Error", errorResponse.getError());
        // Technical exceptions should not expose internal details
        assertEquals("An internal error occurred. Please try again later.", errorResponse.getMessage());
        assertNull(errorResponse.getStackTrace());
    }
    
    /**
     * Test handling of TechnicalException with stack trace enabled
     */
    @Test
    @DisplayName("Should include stack trace when includeStackTrace is true")
    void testHandleTechnicalExceptionWithStackTrace() {
        // Given
        ReflectionTestUtils.setField(globalExceptionHandler, "includeStackTrace", true);
        String message = "Database error";
        Exception cause = new RuntimeException("Connection failed");
        TechnicalException exception = new TechnicalException(message, cause);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTechnicalException(exception, request);
        
        // Then
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertNotNull(errorResponse.getStackTrace());
        assertTrue(errorResponse.getStackTrace().contains("TechnicalException"));
    }
    
    /**
     * Test handling of MethodArgumentNotValidException
     */
    @Test
    @DisplayName("Should handle MethodArgumentNotValidException and return 400")
    void testHandleMethodArgumentNotValid() {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        
        List<FieldError> fieldErrorList = Arrays.asList(
            new FieldError("user", "email", "Invalid email"),
            new FieldError("user", "username", "Username required")
        );
        
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(new ArrayList<>(fieldErrorList));
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleMethodArgumentNotValid(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation failed", errorResponse.getMessage());
        assertEquals("VALIDATION_ERROR", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(2, errorResponse.getFieldErrors().size());
        assertEquals("Invalid email", errorResponse.getFieldErrors().get("email"));
        assertEquals("Username required", errorResponse.getFieldErrors().get("username"));
    }
    
    /**
     * Test handling of ConstraintViolationException
     */
    @Test
    @DisplayName("Should handle ConstraintViolationException and return 400")
    void testHandleConstraintViolation() {
        // Given
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        
        // Create mock violations
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        Path path1 = mock(Path.class);
        when(path1.toString()).thenReturn("email");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("must be a valid email");
        
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        Path path2 = mock(Path.class);
        when(path2.toString()).thenReturn("age");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("must be greater than 0");
        
        violations.add(violation1);
        violations.add(violation2);
        
        ConstraintViolationException exception = new ConstraintViolationException(violations);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleConstraintViolation(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Validation failed", errorResponse.getMessage());
        assertEquals("CONSTRAINT_VIOLATION", errorResponse.getErrorCode());
        assertNotNull(errorResponse.getFieldErrors());
        assertEquals(2, errorResponse.getFieldErrors().size());
    }
    
    /**
     * Test handling of DataIntegrityViolationException with unique constraint
     */
    @Test
    @DisplayName("Should handle DataIntegrityViolationException for unique constraint and return 409")
    void testHandleDataIntegrityViolationUniqueConstraint() {
        // Given
        String message = "ERROR: duplicate key value violates unique constraint";
        DataIntegrityViolationException exception = new DataIntegrityViolationException(message);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(409, errorResponse.getStatus());
        assertEquals("A record with the given values already exists", errorResponse.getMessage());
        assertEquals("DATA_INTEGRITY_VIOLATION", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of DataIntegrityViolationException with foreign key constraint
     */
    @Test
    @DisplayName("Should handle DataIntegrityViolationException for foreign key constraint and return 409")
    void testHandleDataIntegrityViolationForeignKeyConstraint() {
        // Given
        String message = "ERROR: violates foreign key constraint";
        DataIntegrityViolationException exception = new DataIntegrityViolationException(message);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleDataIntegrityViolation(exception, request);
        
        // Then
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("The operation violates referential integrity", errorResponse.getMessage());
    }
    
    /**
     * Test handling of BadCredentialsException
     */
    @Test
    @DisplayName("Should handle BadCredentialsException and return 401")
    void testHandleBadCredentialsException() {
        // Given
        BadCredentialsException exception = new BadCredentialsException("Bad credentials");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(401, errorResponse.getStatus());
        assertEquals("Invalid username or password", errorResponse.getMessage());
        assertEquals("AUTHENTICATION_ERROR", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of generic AuthenticationException
     */
    @Test
    @DisplayName("Should handle generic AuthenticationException and return 401")
    void testHandleGenericAuthenticationException() {
        // Given
        AuthenticationException exception = mock(AuthenticationException.class);
        when(exception.getMessage()).thenReturn("Authentication failed");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAuthenticationException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("Authentication failed", errorResponse.getMessage());
    }
    
    /**
     * Test handling of AccessDeniedException
     */
    @Test
    @DisplayName("Should handle AccessDeniedException and return 403")
    void testHandleAccessDeniedException() {
        // Given
        AccessDeniedException exception = new AccessDeniedException("Access denied");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleAccessDeniedException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(403, errorResponse.getStatus());
        assertEquals("Access denied", errorResponse.getMessage());
        assertEquals("ACCESS_DENIED", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of MethodArgumentTypeMismatchException
     */
    @Test
    @DisplayName("Should handle MethodArgumentTypeMismatchException and return 400")
    void testHandleTypeMismatch() {
        // Given
        MethodArgumentTypeMismatchException exception = mock(MethodArgumentTypeMismatchException.class);
        when(exception.getValue()).thenReturn("abc");
        when(exception.getName()).thenReturn("id");
        when(exception.getRequiredType()).thenReturn((Class) Long.class);
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleTypeMismatch(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(400, errorResponse.getStatus());
        assertEquals("Invalid value 'abc' for parameter 'id'. Expected type: Long", errorResponse.getMessage());
        assertEquals("TYPE_MISMATCH", errorResponse.getErrorCode());
    }
    
    /**
     * Test handling of generic Exception
     */
    @Test
    @DisplayName("Should handle generic Exception and return 500")
    void testHandleGenericException() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleGenericException(exception, request);
        
        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals(500, errorResponse.getStatus());
        assertEquals("An unexpected error occurred", errorResponse.getMessage());
        assertEquals("INTERNAL_ERROR", errorResponse.getErrorCode());
        assertNull(errorResponse.getStackTrace());
    }
    
    /**
     * Test correlation ID generation
     */
    @Test
    @DisplayName("Should generate unique correlation IDs for each error")
    void testCorrelationIdGeneration() {
        // Given
        ResourceNotFoundException exception1 = new ResourceNotFoundException("User", 1L);
        ResourceNotFoundException exception2 = new ResourceNotFoundException("User", 2L);
        
        // When
        ResponseEntity<ErrorResponse> response1 = globalExceptionHandler.handleResourceNotFoundException(exception1, request);
        ResponseEntity<ErrorResponse> response2 = globalExceptionHandler.handleResourceNotFoundException(exception2, request);
        
        // Then
        assertNotNull(response1.getBody().getCorrelationId());
        assertNotNull(response2.getBody().getCorrelationId());
        assertNotEquals(response1.getBody().getCorrelationId(), response2.getBody().getCorrelationId());
        
        // Verify UUID format
        assertTrue(response1.getBody().getCorrelationId().matches(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }
    
    /**
     * Test timestamp is properly set
     */
    @Test
    @DisplayName("Should set timestamp close to current time")
    void testTimestampGeneration() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("User", 1L);
        LocalDateTime before = LocalDateTime.now();
        
        // When
        ResponseEntity<ErrorResponse> response = globalExceptionHandler.handleResourceNotFoundException(exception, request);
        LocalDateTime after = LocalDateTime.now();
        
        // Then
        assertNotNull(response.getBody().getTimestamp());
        LocalDateTime timestamp = response.getBody().getTimestamp();
        assertTrue(timestamp.isAfter(before.minusSeconds(1)));
        assertTrue(timestamp.isBefore(after.plusSeconds(1)));
    }
}