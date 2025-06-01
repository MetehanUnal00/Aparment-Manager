package com.example.apartmentmanagerapi.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for contract renewal requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractRenewalRequest {
    
    /**
     * New end date for the renewed contract
     */
    @NotNull(message = "New end date is required")
    @Future(message = "New end date must be in the future")
    private LocalDate newEndDate;
    
    /**
     * Optional new monthly rent (if different from current)
     */
    @Positive(message = "Monthly rent must be positive if provided")
    @DecimalMin(value = "0.01", message = "Monthly rent must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Monthly rent must have at most 8 integer digits and 2 decimal places")
    private BigDecimal newMonthlyRent;
    
    /**
     * Optional new security deposit
     */
    @PositiveOrZero(message = "Security deposit cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Security deposit must have at most 8 integer digits and 2 decimal places")
    private BigDecimal newSecurityDeposit;
    
    /**
     * Whether to keep the same day of month for dues
     */
    @Builder.Default
    private boolean keepSameDayOfMonth = true;
    
    /**
     * New day of month if changing
     */
    @Min(value = 1, message = "Day of month must be at least 1")
    @Max(value = 31, message = "Day of month must be at most 31")
    private Integer newDayOfMonth;
    
    /**
     * Notes about the renewal
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String renewalNotes;
    
    /**
     * Whether to generate dues for the renewal period immediately
     */
    @Builder.Default
    private boolean generateDuesImmediately = true;
}