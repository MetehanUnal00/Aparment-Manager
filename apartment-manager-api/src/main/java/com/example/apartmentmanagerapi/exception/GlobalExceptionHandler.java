package com.example.apartmentmanagerapi.exception;

import com.example.apartmentmanagerapi.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Global exception handler for the application.
 * Handles all exceptions and returns consistent error responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * Flag to include stack traces in error responses (should be false in production)
     */
    @Value("${app.debug.include-stacktrace:false}")
    private boolean includeStackTrace;
    
    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Resource not found error [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    
    /**
     * Handle ValidationException
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Validation error [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .fieldErrors(ex.hasFieldErrors() ? ex.getFieldErrors() : null)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle DuplicateResourceException
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Duplicate resource error [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle UnauthorizedException
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Authorization error [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle BusinessRuleException
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(
            BusinessRuleException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Business rule violation [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }
    
    /**
     * Handle TechnicalException
     */
    @ExceptionHandler(TechnicalException.class)
    public ResponseEntity<ErrorResponse> handleTechnicalException(
            TechnicalException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Technical error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An internal error occurred. Please try again later.")
                .path(request.getRequestURI())
                .errorCode(ex.getErrorCode())
                .correlationId(correlationId)
                .stackTrace(includeStackTrace ? getStackTrace(ex) : null)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Handle Spring's MethodArgumentNotValidException (Bean Validation)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Method argument validation error [{}]: {}", correlationId, ex.getMessage());
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .errorCode("VALIDATION_ERROR")
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle ConstraintViolationException (Bean Validation on path variables/request params)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Constraint violation error [{}]: {}", correlationId, ex.getMessage());
        
        Map<String, String> fieldErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message("Validation failed")
                .path(request.getRequestURI())
                .errorCode("CONSTRAINT_VIOLATION")
                .correlationId(correlationId)
                .fieldErrors(fieldErrors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle DataIntegrityViolationException (Database constraints)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Data integrity violation [{}]: {}", correlationId, ex.getMessage());
        
        String message = "Data integrity violation";
        
        // Check for specific constraint violations
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("unique constraint") || ex.getMessage().contains("duplicate key")) {
                message = "A record with the given values already exists";
            } else if (ex.getMessage().contains("foreign key constraint")) {
                message = "The operation violates referential integrity";
            }
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode("DATA_INTEGRITY_VIOLATION")
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }
    
    /**
     * Handle Spring Security Authentication exceptions
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Authentication error [{}]: {}", correlationId, ex.getMessage());
        
        String message = "Authentication failed";
        if (ex instanceof BadCredentialsException) {
            message = "Invalid username or password";
        }
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode("AUTHENTICATION_ERROR")
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }
    
    /**
     * Handle Spring Security Access Denied exceptions
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Access denied error [{}]: {}", correlationId, ex.getMessage());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access denied")
                .path(request.getRequestURI())
                .errorCode("ACCESS_DENIED")
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }
    
    /**
     * Handle method argument type mismatch (e.g., invalid path variable type)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Type mismatch error [{}]: {}", correlationId, ex.getMessage());
        
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s",
                ex.getValue(), ex.getName(), ex.getRequiredType().getSimpleName());
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .errorCode("TYPE_MISMATCH")
                .correlationId(correlationId)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle all other exceptions (fallback)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        String correlationId = generateCorrelationId();
        log.error("Unexpected error [{}]: {}", correlationId, ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .errorCode("INTERNAL_ERROR")
                .correlationId(correlationId)
                .stackTrace(includeStackTrace ? getStackTrace(ex) : null)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Generate a unique correlation ID for request tracking
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Convert exception stack trace to string
     */
    private String getStackTrace(Exception ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        return sw.toString();
    }
}