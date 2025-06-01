package com.example.apartmentmanagerapi.exception;

import com.example.apartmentmanagerapi.entity.Contract;

/**
 * Exception thrown when contract modification is not allowed
 */
public class ContractModificationException extends BusinessRuleException {
    
    /**
     * Create exception with message
     * @param message Error message
     */
    public ContractModificationException(String message) {
        super(message);
    }
    
    /**
     * Create exception for invalid contract status
     * @param contractId Contract ID
     * @param currentStatus Current status
     */
    public static ContractModificationException invalidStatus(Long contractId, 
                                                            Contract.ContractStatus currentStatus) {
        return new ContractModificationException(
            String.format("Contract %d cannot be modified in %s status", 
                        contractId, currentStatus)
        );
    }
    
    /**
     * Create exception for expired contract
     * @param contractId Contract ID
     */
    public static ContractModificationException contractExpired(Long contractId) {
        return new ContractModificationException(
            String.format("Contract %d has already expired and cannot be modified", contractId)
        );
    }
    
    /**
     * Create exception for modification date in the past
     * @param contractId Contract ID
     */
    public static ContractModificationException modificationDateInPast(Long contractId) {
        return new ContractModificationException(
            String.format("Contract %d modification date cannot be in the past", contractId)
        );
    }
    
    /**
     * Create exception for contract with paid dues
     * @param contractId Contract ID
     * @param paidDuesCount Number of paid dues
     */
    public static ContractModificationException hasPaidDues(Long contractId, int paidDuesCount) {
        return new ContractModificationException(
            String.format("Contract %d has %d paid dues and cannot be modified. " +
                        "Please create a new contract instead", 
                        contractId, paidDuesCount)
        );
    }
}