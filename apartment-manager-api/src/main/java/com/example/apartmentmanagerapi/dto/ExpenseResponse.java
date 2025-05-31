package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning expense information to clients.
 * Includes all expense details and building information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseResponse {
    
    /**
     * Unique expense ID
     */
    private Long id;
    
    /**
     * Building information
     */
    private BuildingSummary building;
    
    /**
     * Expense category
     */
    private Expense.ExpenseCategory category;
    
    /**
     * Category display name
     */
    private String categoryDisplayName;
    
    /**
     * Expense amount
     */
    private BigDecimal amount;
    
    /**
     * Date of the expense
     */
    private LocalDate expenseDate;
    
    /**
     * Expense description
     */
    private String description;
    
    /**
     * Vendor name
     */
    private String vendorName;
    
    /**
     * Invoice number
     */
    private String invoiceNumber;
    
    /**
     * Whether this is a recurring expense
     */
    private Boolean isRecurring;
    
    /**
     * Recurrence frequency if recurring
     */
    private Expense.RecurrenceFrequency recurrenceFrequency;
    
    /**
     * User who recorded the expense
     */
    private String recordedBy;
    
    /**
     * Timestamp when created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Nested DTO for building summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BuildingSummary {
        private Long id;
        private String name;
        private String address;
    }
    
    /**
     * Convert Expense entity to response DTO
     */
    public static ExpenseResponse fromEntity(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .building(BuildingSummary.builder()
                        .id(expense.getBuilding().getId())
                        .name(expense.getBuilding().getName())
                        .address(expense.getBuilding().getAddress())
                        .build())
                .category(expense.getCategory())
                .categoryDisplayName(expense.getCategory().getDisplayName())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .description(expense.getDescription())
                .vendorName(expense.getVendorName())
                .invoiceNumber(expense.getInvoiceNumber())
                .isRecurring(expense.getIsRecurring())
                .recurrenceFrequency(expense.getRecurrenceFrequency())
                .recordedBy(expense.getRecordedBy() != null ? 
                        expense.getRecordedBy().getUsername() : null)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}