package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for creating or updating a payment.
 * Contains validation rules to ensure data integrity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    
    /**
     * ID of the flat making the payment
     * Required for creating a payment
     */
    @NotNull(message = "Flat ID is required")
    private Long flatId;
    
    /**
     * Payment amount
     * Must be greater than zero
     */
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than zero")
    private BigDecimal amount;
    
    /**
     * Date and time of the payment
     * Defaults to current time if not provided
     */
    private LocalDateTime paymentDate;
    
    /**
     * Payment method used
     */
    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
    
    /**
     * Optional reference number (e.g., bank transaction ID)
     */
    @Size(max = 100, message = "Reference number cannot exceed 100 characters")
    private String referenceNumber;
    
    /**
     * Optional notes about the payment
     */
    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;
    
    /**
     * Optional description for display
     */
    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;
    
    /**
     * Optional receipt number
     */
    @Size(max = 100, message = "Receipt number cannot exceed 100 characters")
    private String receiptNumber;
}