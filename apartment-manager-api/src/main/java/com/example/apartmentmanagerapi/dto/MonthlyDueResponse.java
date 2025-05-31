package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.MonthlyDue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for returning monthly due information to clients.
 * Includes due details and payment status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyDueResponse {
    
    /**
     * Unique due ID
     */
    private Long id;
    
    /**
     * Flat information
     */
    private FlatSummary flat;
    
    /**
     * Total amount due
     */
    private BigDecimal dueAmount;
    
    /**
     * Due date
     */
    private LocalDate dueDate;
    
    /**
     * Current status
     */
    private MonthlyDue.DueStatus status;
    
    /**
     * Description of the due
     */
    private String dueDescription;
    
    /**
     * Amount paid so far
     */
    private BigDecimal paidAmount;
    
    /**
     * Date when payment was made (if paid)
     */
    private LocalDateTime paymentDate;
    
    /**
     * Date when fully paid (if applicable)
     */
    private LocalDate paidDate;
    
    /**
     * Base rent amount
     */
    private BigDecimal baseRent;
    
    /**
     * Additional charges
     */
    private BigDecimal additionalCharges;
    
    /**
     * Additional charges description
     */
    private String additionalChargesDescription;
    
    /**
     * Whether this due is overdue
     */
    private boolean isOverdue;
    
    /**
     * Timestamp when created
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when last updated
     */
    private LocalDateTime updatedAt;
    
    /**
     * Nested DTO for flat summary
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlatSummary {
        private Long id;
        private String flatNumber;
        private String tenantName;
        private String tenantContact;
    }
    
    /**
     * Convert MonthlyDue entity to response DTO
     */
    public static MonthlyDueResponse fromEntity(MonthlyDue due) {
        return MonthlyDueResponse.builder()
                .id(due.getId())
                .flat(FlatSummary.builder()
                        .id(due.getFlat().getId())
                        .flatNumber(due.getFlat().getFlatNumber())
                        .tenantName(due.getFlat().getTenantName())
                        .tenantContact(due.getFlat().getTenantContact())
                        .build())
                .dueAmount(due.getDueAmount())
                .dueDate(due.getDueDate())
                .status(due.getStatus())
                .dueDescription(due.getDueDescription())
                .paidAmount(due.getPaidAmount())
                .paymentDate(due.getPaymentDate())
                .paidDate(due.getPaidDate())
                .baseRent(due.getBaseRent())
                .additionalCharges(due.getAdditionalCharges())
                .additionalChargesDescription(due.getAdditionalChargesDescription())
                .isOverdue(due.isOverdue())
                .createdAt(due.getCreatedAt())
                .updatedAt(due.getUpdatedAt())
                .build();
    }
}