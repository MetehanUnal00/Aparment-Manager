package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.dto.validation.AtLeastOneNotNull;
import com.example.apartmentmanagerapi.validation.FutureOrToday;
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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@AtLeastOneNotNull(fields = {"flatId", "buildingId"}, message = "Either flatId or buildingId must be provided")
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
     * Due amount to charge
     */
    @NotNull(message = "Due amount is required")
    @DecimalMin(value = "0.01", message = "Due amount must be greater than zero")
    private BigDecimal dueAmount;
    
    /**
     * Due date for payment
     */
    @NotNull(message = "Due date is required")
    @FutureOrToday(message = "Due date must be today or in the future")
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
}