package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for returning payment information to clients.
 * Includes all payment details and related information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    
    /**
     * Unique payment ID
     */
    private Long id;
    
    /**
     * Flat information
     */
    private FlatSummary flat;
    
    /**
     * Payment amount
     */
    private BigDecimal amount;
    
    /**
     * Date and time of payment
     */
    private LocalDateTime paymentDate;
    
    /**
     * Payment method used
     */
    private Payment.PaymentMethod paymentMethod;
    
    /**
     * Reference number if provided
     */
    private String referenceNumber;
    
    /**
     * Payment notes
     */
    private String notes;
    
    /**
     * Payment description
     */
    private String description;
    
    /**
     * Receipt number
     */
    private String receiptNumber;
    
    /**
     * User who recorded the payment
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
     * Version for optimistic locking
     */
    private Integer version;
    
    /**
     * Nested DTO for flat summary information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FlatSummary {
        private Long id;
        private String flatNumber;
        private String tenantName;
        private Long buildingId;
        private String buildingName;
    }
    
    /**
     * Convert Payment entity to PaymentResponse DTO
     */
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .flat(FlatSummary.builder()
                        .id(payment.getFlat().getId())
                        .flatNumber(payment.getFlat().getFlatNumber())
                        .tenantName(payment.getFlat().getTenantName())
                        .buildingId(payment.getFlat().getApartmentBuilding().getId())
                        .buildingName(payment.getFlat().getApartmentBuilding().getName())
                        .build())
                .amount(payment.getAmount())
                .paymentDate(payment.getPaymentDate())
                .paymentMethod(payment.getPaymentMethod())
                .referenceNumber(payment.getReferenceNumber())
                .notes(payment.getNotes())
                .description(payment.getDescription())
                .receiptNumber(payment.getReceiptNumber())
                .recordedBy(payment.getRecordedBy() != null ? 
                        payment.getRecordedBy().getUsername() : null)
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .version(payment.getVersion())
                .build();
    }
}