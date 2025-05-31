package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Expense;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating an expense.
 * Includes expense details and distribution options.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseRequest {
    
    /**
     * ID of the building this expense belongs to
     */
    @NotNull(message = "Building ID is required")
    private Long buildingId;
    
    /**
     * Expense category
     */
    @NotNull(message = "Expense category is required")
    private Expense.ExpenseCategory category;
    
    /**
     * Expense amount
     */
    @NotNull(message = "Expense amount is required")
    @DecimalMin(value = "0.01", message = "Expense amount must be greater than zero")
    private BigDecimal amount;
    
    /**
     * Date of the expense
     */
    @NotNull(message = "Expense date is required")
    private LocalDate expenseDate;
    
    /**
     * Description of the expense
     */
    @NotNull(message = "Description is required")
    @Size(min = 3, max = 500, message = "Description must be between 3 and 500 characters")
    private String description;
    
    /**
     * Vendor or service provider name
     */
    @Size(max = 100, message = "Vendor name cannot exceed 100 characters")
    private String vendorName;
    
    /**
     * Invoice number for reference
     */
    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    private String invoiceNumber;
    
    /**
     * Whether this is a recurring expense
     */
    @Builder.Default
    private Boolean isRecurring = false;
    
    /**
     * Recurrence frequency if recurring
     */
    private Expense.RecurrenceFrequency recurrenceFrequency;
    
    /**
     * Whether to distribute this expense to flats
     * If true, monthly dues will be created for each flat
     */
    @Builder.Default
    private Boolean distributeToFlats = false;
}