package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Expense entity represents expenses incurred for an apartment building.
 * This includes maintenance, utilities, repairs, and other operational costs.
 * Expenses are tracked at the building level for proper cost allocation and reporting.
 */
@Entity
@Table(name = "expenses", 
    indexes = {
        @Index(name = "idx_expense_building_date", columnList = "building_id, expense_date"),
        @Index(name = "idx_expense_category", columnList = "expense_category")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {
    
    /**
     * Unique identifier for the expense
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The apartment building this expense belongs to.
     * Many expenses can belong to one building (one-to-many relationship).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private ApartmentBuilding building;
    
    /**
     * Category of the expense for reporting and analysis
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false, length = 50)
    private ExpenseCategory category;
    
    /**
     * Amount of the expense.
     * Using BigDecimal for precise monetary calculations.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    
    /**
     * Date when the expense was incurred
     */
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;
    
    /**
     * Brief description of the expense
     */
    @Column(nullable = false, length = 255)
    private String description;
    
    /**
     * Detailed notes about the expense (optional)
     * Can include vendor information, invoice numbers, etc.
     */
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    /**
     * Name of the vendor or service provider (optional)
     */
    @Column(name = "vendor_name", length = 100)
    private String vendorName;
    
    /**
     * Invoice or receipt number for reference (optional)
     */
    @Column(name = "invoice_number", length = 50)
    private String invoiceNumber;
    
    /**
     * User who recorded this expense
     * Helps with audit trail and accountability
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedBy;
    
    /**
     * Whether this is a recurring expense
     * Helps identify regular monthly/annual expenses
     */
    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;
    
    /**
     * Frequency of recurring expense (if applicable)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recurrence_frequency", length = 20)
    private RecurrenceFrequency recurrenceFrequency;
    
    /**
     * Timestamp when this record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * Automatically set creation timestamp before persisting
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expenseDate == null) {
            expenseDate = LocalDate.now();
        }
        if (isRecurring == null) {
            isRecurring = false;
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
     * Categories for organizing expenses
     */
    public enum ExpenseCategory {
        MAINTENANCE("Maintenance"),           // Regular maintenance and repairs
        UTILITIES("Utilities"),              // Water, electricity, gas, etc.
        CLEANING("Cleaning"),                // Cleaning services
        SECURITY("Security"),                // Security services and systems
        INSURANCE("Insurance"),              // Building insurance
        TAXES("Taxes"),                      // Property taxes
        MANAGEMENT("Management"),            // Management fees
        REPAIRS("Repairs"),                  // Major repairs
        LANDSCAPING("Landscaping"),          // Garden and outdoor maintenance
        ELEVATOR("Elevator"),                // Elevator maintenance and repairs
        SUPPLIES("Supplies"),                // Office and cleaning supplies
        LEGAL("Legal"),                      // Legal fees
        ACCOUNTING("Accounting"),            // Accounting and bookkeeping
        MARKETING("Marketing"),              // Advertising for vacant units
        OTHER("Other");                      // Miscellaneous expenses
        
        private final String displayName;
        
        ExpenseCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Frequency options for recurring expenses
     */
    public enum RecurrenceFrequency {
        WEEKLY("Weekly"),
        MONTHLY("Monthly"),
        QUARTERLY("Quarterly"),
        SEMI_ANNUAL("Semi-Annual"),
        ANNUAL("Annual");
        
        private final String displayName;
        
        RecurrenceFrequency(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}