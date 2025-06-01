package com.example.apartmentmanagerapi.dto;

import com.example.apartmentmanagerapi.entity.Contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lightweight DTO for contract list views
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractSummaryResponse {
    
    /**
     * Contract identification
     */
    private Long id;
    private Long flatId;
    private String flatNumber;
    private String buildingName;
    
    /**
     * Tenant information
     */
    private String tenantName;
    
    /**
     * Contract period
     */
    private LocalDate startDate;
    private LocalDate endDate;
    
    /**
     * Financial summary
     */
    private BigDecimal monthlyRent;
    private BigDecimal outstandingBalance;
    
    /**
     * Status information
     */
    private Contract.ContractStatus status;
    private boolean isExpiringSoon;
    private Integer daysUntilExpiry;
    private boolean hasOverdueDues;
    
    /**
     * Quick indicators
     */
    private boolean isCurrentlyActive;
    private Integer contractLengthInMonths;
    private String statusBadgeColor; // For UI
}