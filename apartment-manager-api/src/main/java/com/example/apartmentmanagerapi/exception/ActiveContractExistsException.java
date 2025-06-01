package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when trying to create a contract for a flat that already has an active contract
 */
public class ActiveContractExistsException extends BusinessRuleException {
    
    /**
     * Create exception with message
     * @param message Error message
     */
    public ActiveContractExistsException(String message) {
        super(message);
    }
    
    /**
     * Create exception for flat with existing active contract
     * @param flatId Flat ID
     */
    public ActiveContractExistsException(Long flatId) {
        super(String.format("Flat %d already has an active contract. " +
                          "Please cancel or wait for the existing contract to expire before creating a new one", 
                          flatId));
    }
    
    /**
     * Create exception with contract details
     * @param flatId Flat ID
     * @param existingContractId Existing contract ID
     */
    public ActiveContractExistsException(Long flatId, Long existingContractId) {
        super(String.format("Flat %d already has an active contract (ID: %d). " +
                          "Please cancel or wait for the existing contract to expire before creating a new one", 
                          flatId, existingContractId));
    }
}