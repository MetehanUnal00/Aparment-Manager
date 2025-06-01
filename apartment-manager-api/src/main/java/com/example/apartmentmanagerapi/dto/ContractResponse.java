package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for contract responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractResponse {
    
    /**
     * Contract ID
     */
    private Long id;
    
    /**
     * Flat information
     */
    private Long flatId;
    private String flatNumber;
    private String buildingName;
    
    /**
     * Contract dates
     */
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer contractLengthInMonths;
    
    /**
     * Financial information
     */
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    private Integer dayOfMonth;
    
    /**
     * Contract status
     */
    private Contract.ContractStatus status;
    private String statusDisplayName;
    
    /**
     * Tenant information
     */
    private String tenantName;
    private String tenantContact;
    private String tenantEmail;
    
    /**
     * Related contracts
     */
    private Long previousContractId;
    private boolean hasRenewal;
    
    /**
     * Cancellation information
     */
    private String cancellationReason;
    private LocalDateTime cancellationDate;
    private String cancelledByUsername;
    
    /**
     * Due generation information
     */
    private Integer totalDuesGenerated;
    private Integer paidDuesCount;
    private Integer unpaidDuesCount;
    private BigDecimal totalAmountDue;
    private BigDecimal totalAmountPaid;
    private BigDecimal outstandingBalance;
    
    /**
     * Contract health indicators
     */
    private boolean isExpiringSoon; // Within 30 days
    private Integer daysUntilExpiry;
    private boolean hasOverdueDues;
    private LocalDate nextDueDate;
    
    /**
     * Additional information
     */
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    /**
     * Calculated fields
     */
    private boolean isCurrentlyActive;
    private boolean canBeRenewed;
    private boolean canBeModified;
    private boolean canBeCancelled;
}