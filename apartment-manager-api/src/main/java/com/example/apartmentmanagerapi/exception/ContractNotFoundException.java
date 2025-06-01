package com.example.apartmentmanagerapi.exception;

/**
 * Exception thrown when a contract is not found
 */
public class ContractNotFoundException extends ResourceNotFoundException {
    
    /**
     * Create exception for contract not found by ID
     * @param contractId Contract ID
     */
    public ContractNotFoundException(Long contractId) {
        super("Contract", contractId);
    }
    
    /**
     * Create exception for no active contract found
     * @param flatId Flat ID
     */
    public static ContractNotFoundException noActiveContract(Long flatId) {
        String message = String.format("No active contract found for flat %d", flatId);
        return new ContractNotFoundException(message, "Contract", "active-for-flat-" + flatId);
    }
    
    /**
     * Private constructor for custom messages
     * @param message Custom error message
     * @param resourceType Resource type
     * @param resourceId Resource identifier
     */
    private ContractNotFoundException(String message, String resourceType, Object resourceId) {
        super(message, resourceType, resourceId);
    }
}