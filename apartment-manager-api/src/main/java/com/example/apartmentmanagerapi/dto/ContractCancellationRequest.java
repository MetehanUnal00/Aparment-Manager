package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Contract;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for contract cancellation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractCancellationRequest {
    
    /**
     * Reason for cancellation
     */
    @NotNull(message = "Cancellation reason category is required")
    private CancellationReasonCategory reasonCategory;
    
    /**
     * Detailed cancellation reason
     */
    @NotBlank(message = "Cancellation reason is required")
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;
    
    /**
     * Effective date of cancellation (optional, defaults to today)
     */
    private LocalDate effectiveDate;
    
    /**
     * Whether to cancel all unpaid dues
     */
    @Builder.Default
    private boolean cancelUnpaidDues = true;
    
    /**
     * Whether to refund security deposit
     */
    @Builder.Default
    private boolean refundSecurityDeposit = false;
    
    /**
     * Amount to deduct from security deposit (if any)
     */
    private BigDecimal securityDepositDeduction;
    
    /**
     * Notes about the cancellation
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    /**
     * Predefined cancellation reason categories
     */
    public enum CancellationReasonCategory {
        TENANT_REQUEST("Tenant Request"),
        NON_PAYMENT("Non-Payment"),
        BREACH_OF_CONTRACT("Breach of Contract"),
        PROPERTY_DAMAGE("Property Damage"),
        MUTUAL_AGREEMENT("Mutual Agreement"),
        BUILDING_CLOSURE("Building Closure"),
        RENOVATION("Renovation Required"),
        OTHER("Other");
        
        private final String displayName;
        
        CancellationReasonCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}