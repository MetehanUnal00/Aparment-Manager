package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for contract modification requests (e.g., rent changes)
 * Creates a new contract that supersedes the current one
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractModificationRequest {
    
    /**
     * Effective date of the modification
     */
    @NotNull(message = "Effective date is required")
    @FutureOrPresent(message = "Effective date cannot be in the past")
    private LocalDate effectiveDate;
    
    /**
     * New monthly rent amount
     */
    @NotNull(message = "New monthly rent is required")
    @Positive(message = "Monthly rent must be positive")
    @DecimalMin(value = "0.01", message = "Monthly rent must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Monthly rent must have at most 8 integer digits and 2 decimal places")
    private BigDecimal newMonthlyRent;
    
    /**
     * Reason for modification
     */
    @NotNull(message = "Modification reason is required")
    private ModificationReason reason;
    
    /**
     * Detailed explanation
     */
    @NotBlank(message = "Modification details are required")
    @Size(max = 500, message = "Modification details must not exceed 500 characters")
    private String modificationDetails;
    
    /**
     * Whether to keep other contract terms unchanged
     */
    @Builder.Default
    private boolean keepOtherTerms = true;
    
    /**
     * Optional new security deposit
     */
    @PositiveOrZero(message = "Security deposit cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Security deposit must have at most 8 integer digits and 2 decimal places")
    private BigDecimal newSecurityDeposit;
    
    /**
     * Optional new day of month
     */
    @Min(value = 1, message = "Day of month must be at least 1")
    @Max(value = 31, message = "Day of month must be at most 31")
    private Integer newDayOfMonth;
    
    /**
     * Notes about the modification
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    /**
     * Optional new end date
     */
    private LocalDate endDate;
    
    /**
     * Whether to regenerate dues
     */
    @Builder.Default
    private boolean regenerateDues = true;
    
    /**
     * Predefined modification reasons
     */
    public enum ModificationReason {
        ANNUAL_INCREASE("Annual Rent Increase"),
        MARKET_ADJUSTMENT("Market Rate Adjustment"),
        NEGOTIATED_CHANGE("Negotiated Change"),
        SERVICE_ADDITION("Additional Services"),
        SERVICE_REMOVAL("Removed Services"),
        ERROR_CORRECTION("Error Correction"),
        OTHER("Other");
        
        private final String displayName;
        
        ModificationReason(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}