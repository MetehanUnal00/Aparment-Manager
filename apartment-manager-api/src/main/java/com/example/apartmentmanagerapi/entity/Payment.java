package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment entity represents a payment made by a tenant for their flat.
 * This entity tracks all payment transactions including amount, date, and payment method.
 * 
 * Implements optimistic locking using @Version to handle concurrent payment updates safely.
 * This prevents issues when multiple users try to update the same payment record simultaneously.
 */
@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    /**
     * Unique identifier for the payment
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The flat associated with this payment.
     * Many payments can belong to one flat (one-to-many relationship).
     * FetchType.LAZY is used to optimize performance by loading flat data only when needed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;
    
    /**
     * Payment amount in the system's currency.
     * Using BigDecimal for precise monetary calculations to avoid floating-point errors.
     * Column precision of 10 with scale of 2 allows values up to 99,999,999.99
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * Date and time when the payment was made.
     * Automatically set to current timestamp when payment is created.
     */
    @Column(name = "payment_date", nullable = false)
    @Builder.Default
    private LocalDateTime paymentDate = LocalDateTime.now();
    
    /**
     * Method used for payment (e.g., CASH, BANK_TRANSFER, CREDIT_CARD, etc.)
     * Using enum for type safety and consistency
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;
    
    /**
     * Optional reference number for the payment (e.g., bank transaction ID, check number)
     * Can be used for tracking and reconciliation
     */
    @Column(name = "reference_number", length = 100)
    private String referenceNumber;
    
    /**
     * Optional notes or description about the payment
     * Can include details like which month(s) this payment covers
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Description of the payment for display purposes
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * Receipt number if applicable
     */
    @Column(name = "receipt_number", length = 100)
    private String receiptNumber;
    
    /**
     * User who recorded this payment (typically a manager or admin)
     * Helps with audit trail and accountability
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id")
    private User recordedBy;
    
    /**
     * Timestamp when this record was created
     * Automatically managed by JPA
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this record was last updated
     * Automatically managed by JPA
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Version field for optimistic locking.
     * JPA automatically increments this value on each update.
     * If two users try to update the same payment simultaneously,
     * the second update will fail with OptimisticLockException.
     */
    @Version
    @Column(name = "version")
    private Integer version;
    
    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }
    
    /**
     * Automatically update the timestamp before updating
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Payment methods supported by the system
     */
    public enum PaymentMethod {
        CASH("Cash"),
        BANK_TRANSFER("Bank Transfer"),
        CREDIT_CARD("Credit Card"),
        DEBIT_CARD("Debit Card"),
        CHECK("Check"),
        ONLINE_PAYMENT("Online Payment"),
        OTHER("Other");
        
        private final String displayName;
        
        PaymentMethod(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}