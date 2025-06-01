package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a contract overlaps with existing contracts
 */
public class ContractOverlapException extends BusinessRuleException {
    
    /**
     * Create exception with message
     * @param message Error message
     */
    public ContractOverlapException(String message) {
        super(message);
    }
    
    /**
     * Create exception for specific flat
     * @param flatId Flat ID
     * @param details Overlap details
     */
    public ContractOverlapException(Long flatId, String details) {
        super(String.format("Contract overlap detected for flat %d: %s", flatId, details));
    }
}