package com.example.apartmentmanagerapi.validation;

import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Validator to check for contract date overlaps
 * Used by the service layer to validate new contracts
 */
@Component
@RequiredArgsConstructor
public class ContractOverlapValidator {
    
    private final ContractRepository contractRepository;
    
    /**
     * Validate that a new contract doesn't overlap with existing ones
     * @param flatId Flat ID
     * @param startDate Proposed start date
     * @param endDate Proposed end date
     * @param excludeContractId Contract ID to exclude (for modifications)
     * @return Validation result with details
     */
    public ValidationResult validateNoOverlap(Long flatId, LocalDate startDate, 
                                            LocalDate endDate, Long excludeContractId) {
        
        // Find any overlapping contracts
        List<Contract> overlappingContracts = contractRepository.findOverlappingContracts(
            flatId, startDate, endDate, 
            excludeContractId != null ? excludeContractId : -1L
        );
        
        if (overlappingContracts.isEmpty()) {
            return ValidationResult.valid();
        }
        
        // Build detailed error message
        StringBuilder errorMessage = new StringBuilder("Contract dates overlap with existing contracts: ");
        for (Contract contract : overlappingContracts) {
            errorMessage.append(String.format("\n- Contract from %s to %s (Status: %s)",
                contract.getStartDate(), contract.getEndDate(), contract.getStatus()));
        }
        
        return ValidationResult.invalid(errorMessage.toString());
    }
    
    /**
     * Check if a flat already has an active contract
     * @param flatId Flat ID
     * @return true if active contract exists
     */
    public boolean hasActiveContract(Long flatId) {
        return contractRepository.existsByFlatIdAndStatus(flatId, Contract.ContractStatus.ACTIVE);
    }
    
    /**
     * Validation result wrapper
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}