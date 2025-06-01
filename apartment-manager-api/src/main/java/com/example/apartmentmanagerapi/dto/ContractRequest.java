package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.validation.ValidContractDates;
import com.example.apartmentmanagerapi.validation.ValidDayOfMonth;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating a new contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidContractDates // Custom validation for date logic
public class ContractRequest {
    
    /**
     * ID of the flat this contract is for
     */
    @NotNull(message = "Flat ID is required")
    @Positive(message = "Flat ID must be positive")
    private Long flatId;
    
    /**
     * Contract start date
     */
    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date cannot be in the past")
    private LocalDate startDate;
    
    /**
     * Contract end date
     */
    @NotNull(message = "End date is required")
    @Future(message = "End date must be in the future")
    private LocalDate endDate;
    
    /**
     * Monthly rent amount
     */
    @NotNull(message = "Monthly rent is required")
    @Positive(message = "Monthly rent must be positive")
    @DecimalMin(value = "0.01", message = "Monthly rent must be at least 0.01")
    @Digits(integer = 8, fraction = 2, message = "Monthly rent must have at most 8 integer digits and 2 decimal places")
    private BigDecimal monthlyRent;
    
    /**
     * Day of month when rent is due
     */
    @NotNull(message = "Day of month is required")
    @ValidDayOfMonth // Custom validator for 1-31
    private Integer dayOfMonth;
    
    /**
     * Security deposit amount
     */
    @PositiveOrZero(message = "Security deposit cannot be negative")
    @Digits(integer = 8, fraction = 2, message = "Security deposit must have at most 8 integer digits and 2 decimal places")
    private BigDecimal securityDeposit;
    
    /**
     * Tenant information
     */
    @NotBlank(message = "Tenant name is required")
    @Size(max = 100, message = "Tenant name must not exceed 100 characters")
    private String tenantName;
    
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Size(max = 50, message = "Tenant contact must not exceed 50 characters")
    private String tenantContact;
    
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Tenant email must not exceed 100 characters")
    private String tenantEmail;
    
    /**
     * Optional notes about the contract
     */
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    /**
     * Whether to generate monthly dues immediately
     */
    @Builder.Default
    private boolean generateDuesImmediately = true;
}