package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.dto.validation.AtLeastOneNotNull;
import com.example.apartmentmanagerapi.dto.validation.ValidMonthlyDueGeneration;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating monthly dues.
 * Used for both individual and bulk due generation.
 * Either flatId or buildingId must be provided.
 * 
 * Supports two generation modes:
 * 1. Uniform mode (default): Uses dueAmount for all flats
 * 2. Rent-based mode: Uses each flat's monthlyRent with fallback options
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AtLeastOneNotNull(fields = {"flatId", "buildingId"}, message = "Either flatId or buildingId must be provided")
@ValidMonthlyDueGeneration
public class MonthlyDueRequest {
    
    /**
     * For individual due creation - specific flat ID
     */
    private Long flatId;
    
    /**
     * For bulk generation - building ID
     */
    private Long buildingId;
    
    /**
     * Due amount for uniform generation mode (when useFlatsMonthlyRent=false).
     * Also serves as the ultimate fallback when useFlatsMonthlyRent=true 
     * and both flat.monthlyRent and fallbackAmount are invalid/null.
     * Required when useFlatsMonthlyRent=false, validated by @ValidMonthlyDueGeneration.
     */
    private BigDecimal dueAmount;
    
    /**
     * Due date for payment
     */
    @NotNull(message = "Due date is required")
    private LocalDate dueDate;
    
    /**
     * Description of what the due is for
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String dueDescription;
    
    /**
     * Base rent amount (optional)
     */
    @PositiveOrZero(message = "Base rent must be zero or positive")
    private BigDecimal baseRent;
    
    /**
     * Additional charges amount (optional)
     */
    @PositiveOrZero(message = "Additional charges must be zero or positive")
    private BigDecimal additionalCharges;
    
    /**
     * Additional charges description (optional)
     */
    @Size(max = 500, message = "Additional charges description cannot exceed 500 characters")
    private String additionalChargesDescription;
    
    /**
     * Generation mode flag. When true, uses each flat's monthlyRent.
     * When false (default), uses the uniform dueAmount for all flats.
     */
    @Builder.Default
    private Boolean useFlatsMonthlyRent = false;
    
    /**
     * Primary fallback amount when useFlatsMonthlyRent=true and a flat has no valid monthlyRent.
     * If this is also null/invalid, dueAmount will be used as the ultimate fallback.
     * Optional field, only relevant when useFlatsMonthlyRent=true.
     */
    private BigDecimal fallbackAmount;
}