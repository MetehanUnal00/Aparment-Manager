package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when optimistic locking fails
 */
public class OptimisticLockingFailureException extends RuntimeException {
    
    public OptimisticLockingFailureException(String message) {
        super(message);
    }
    
    public OptimisticLockingFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}