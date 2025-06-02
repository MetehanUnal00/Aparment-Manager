package com.example.apartmentmanagerapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for active contract information embedded in FlatResponse
 * This provides tenant and contract details for occupied flats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ActiveContractInfo {
    
    /**
     * Contract identifier
     */
    private Long contractId;
    
    /**
     * Tenant information
     */
    private String tenantName;
    private String tenantEmail;
    private String tenantContact;
    
    /**
     * Financial information
     */
    private BigDecimal monthlyRent;
    private BigDecimal securityDeposit;
    
    /**
     * Contract dates
     */
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate moveInDate; // Same as startDate but clearer naming for UI
    
    /**
     * Contract health indicators
     */
    private Integer daysUntilExpiry;
    private boolean isExpiringSoon;
    private String contractStatus;
    
    /**
     * Payment status
     */
    private BigDecimal outstandingBalance;
    private boolean hasOverdueDues;
}