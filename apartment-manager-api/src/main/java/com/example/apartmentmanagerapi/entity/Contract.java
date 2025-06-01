package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contract entity represents rental agreements for flats.
 * Supports contract history, renewals, and modifications.
 * Each flat can have multiple contracts but only one active at a time.
 */
@Entity
@Table(name = "contracts",
    indexes = {
        @Index(name = "idx_contract_flat_status", columnList = "flat_id, status"),
        @Index(name = "idx_contract_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_contract_status", columnList = "status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"flat", "monthlyDues", "previousContract"})
public class Contract {
    
    /**
     * Unique identifier for the contract
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;
    
    /**
     * The flat this contract belongs to
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "flat_id", nullable = false)
    private Flat flat;
    
    /**
     * Start date of the contract
     */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    
    /**
     * End date of the contract
     */
    @NotNull
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    
    /**
     * Monthly rent amount for this contract period
     */
    @NotNull
    @Positive
    @Column(name = "monthly_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal monthlyRent;
    
    /**
     * Day of month when rent is due (1-31)
     */
    @NotNull
    @Column(name = "day_of_month", nullable = false)
    private Integer dayOfMonth;
    
    /**
     * Current status of the contract
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ContractStatus status = ContractStatus.PENDING;
    
    /**
     * Reference to previous contract (for renewals/modifications)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_contract_id")
    private Contract previousContract;
    
    /**
     * Reason for cancellation (if cancelled)
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;
    
    /**
     * Date when contract was cancelled
     */
    @Column(name = "cancellation_date")
    private LocalDateTime cancellationDate;
    
    /**
     * User who cancelled the contract
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledBy;
    
    /**
     * Monthly dues generated for this contract
     */
    @OneToMany(mappedBy = "contract", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<MonthlyDue> monthlyDues = new HashSet<>();
    
    /**
     * Notes about the contract
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Tenant information snapshot at contract creation
     */
    @Column(name = "tenant_name", length = 100)
    private String tenantName;
    
    @Column(name = "tenant_contact", length = 50)
    private String tenantContact;
    
    @Column(name = "tenant_email", length = 100)
    private String tenantEmail;
    
    /**
     * Security deposit for this contract
     */
    @Column(name = "security_deposit", precision = 10, scale = 2)
    private BigDecimal securityDeposit;
    
    /**
     * Deposit amount (alias for security deposit for test compatibility)
     */
    @Transient
    private BigDecimal depositAmount;
    
    /**
     * Reference to the tenant user
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_user_id")
    private User tenant;
    
    /**
     * Whether this contract auto-renews
     */
    @Column(name = "auto_renew")
    @Builder.Default
    private boolean autoRenew = false;
    
    /**
     * Whether dues have been generated for this contract
     */
    @Column(name = "dues_generated")
    @Builder.Default
    private boolean duesGenerated = false;
    
    /**
     * When the status was last changed
     */
    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;
    
    /**
     * Who changed the status
     */
    @Column(name = "status_changed_by", length = 100)
    private String statusChangedBy;
    
    /**
     * Reason for status change
     */
    @Column(name = "status_change_reason", length = 500)
    private String statusChangeReason;
    
    /**
     * Timestamp when this record was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this record was last updated
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Contract status enumeration
     */
    public enum ContractStatus {
        PENDING("Pending"),           // Future start date
        ACTIVE("Active"),            // Currently in effect
        EXPIRED("Expired"),          // Past end date
        CANCELLED("Cancelled"),      // Manually cancelled
        RENEWED("Renewed"),          // Replaced by renewal
        SUPERSEDED("Superseded");    // Replaced by modification
        
        private final String displayName;
        
        ContractStatus(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * Check if this status represents a terminated contract
         */
        public boolean isTerminated() {
            return this == EXPIRED || this == CANCELLED || 
                   this == RENEWED || this == SUPERSEDED;
        }
    }
    
    /**
     * Check if contract is currently active based on dates and status
     */
    public boolean isCurrentlyActive() {
        if (status != ContractStatus.ACTIVE) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
    
    /**
     * Check if contract dates overlap with another period
     */
    public boolean overlapsWithPeriod(LocalDate otherStart, LocalDate otherEnd) {
        // Contracts overlap if one starts before the other ends
        return !endDate.isBefore(otherStart) && !otherEnd.isBefore(startDate);
    }
    
    /**
     * Calculate the number of months in the contract
     */
    public long getContractLengthInMonths() {
        return java.time.Period.between(startDate, endDate).toTotalMonths() + 1;
    }
    
    /**
     * Get the adjusted due date for a specific month
     */
    public LocalDate getAdjustedDueDateForMonth(LocalDate monthDate) {
        int lastDayOfMonth = monthDate.lengthOfMonth();
        int adjustedDay = Math.min(dayOfMonth, lastDayOfMonth);
        return monthDate.withDayOfMonth(adjustedDay);
    }
    
    /**
     * Check if contract is currently active (simpler version)
     */
    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }
    
    /**
     * Calculate total contract value based on monthly rent and duration
     */
    public BigDecimal calculateTotalContractValue() {
        if (monthlyRent == null || startDate == null || endDate == null) {
            return BigDecimal.ZERO;
        }
        
        // Calculate months between start and end date
        long months = java.time.Period.between(startDate, endDate).toTotalMonths();
        if (endDate.isEqual(startDate)) {
            months = 0;
        } else if (endDate.getDayOfMonth() >= startDate.getDayOfMonth()) {
            months += 1;
        }
        
        return monthlyRent.multiply(BigDecimal.valueOf(Math.max(0, months)));
    }
    
    /**
     * Calculate total contract value including deposit
     */
    public BigDecimal calculateTotalWithDeposit() {
        BigDecimal total = calculateTotalContractValue();
        BigDecimal deposit = getDepositAmount();
        if (deposit != null) {
            total = total.add(deposit);
        }
        return total;
    }
    
    /**
     * Calculate outstanding balance from unpaid monthly dues
     */
    public BigDecimal calculateOutstandingBalance() {
        if (monthlyDues == null || monthlyDues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return monthlyDues.stream()
            .filter(due -> !due.isPaid())
            .map(MonthlyDue::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if contract can be modified (not after dues generated)
     */
    public boolean isModifiable() {
        return !duesGenerated && status != ContractStatus.CANCELLED 
            && status != ContractStatus.EXPIRED && status != ContractStatus.SUPERSEDED;
    }
    
    /**
     * Check if contract is expiring within specified days
     */
    public boolean isExpiringInDays(int days) {
        if (endDate == null || status != ContractStatus.ACTIVE) {
            return false;
        }
        
        LocalDate expiryThreshold = LocalDate.now().plusDays(days);
        return !endDate.isAfter(expiryThreshold);
    }
    
    /**
     * Check if contract is eligible for auto-renewal
     */
    public boolean isEligibleForAutoRenewal() {
        return autoRenew && status == ContractStatus.ACTIVE 
            && endDate != null && !endDate.isBefore(LocalDate.now());
    }
    
    /**
     * Get deposit amount (uses security deposit if depositAmount not set)
     */
    public BigDecimal getDepositAmount() {
        return depositAmount != null ? depositAmount : securityDeposit;
    }
    
    /**
     * Set deposit amount (also sets security deposit)
     */
    public void setDepositAmount(BigDecimal amount) {
        this.depositAmount = amount;
        this.securityDeposit = amount;
    }
}