package com.example.apartmentmanagerapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO for contract expiry notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractExpiryNotification {
    
    /**
     * Contract details
     */
    private Long contractId;
    private Long flatId;
    private String flatNumber;
    private String buildingName;
    private Long buildingId;
    
    /**
     * Tenant information
     */
    private String tenantName;
    private String tenantEmail;
    private String tenantContact;
    
    /**
     * Expiry information
     */
    private LocalDate endDate;
    private Integer daysUntilExpiry;
    private String urgencyLevel; // URGENT (< 7 days), WARNING (< 14 days), INFO (< 30 days)
    
    /**
     * Contract summary
     */
    private BigDecimal monthlyRent;
    private BigDecimal outstandingBalance;
    private boolean hasOverdueDues;
    
    /**
     * Manager information
     */
    private List<ManagerInfo> assignedManagers;
    
    /**
     * Action suggestions
     */
    private boolean renewalRecommended;
    private String recommendedAction;
    
    /**
     * Nested DTO for manager information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ManagerInfo {
        private Long userId;
        private String username;
        private String email;
        private String fullName;
    }
}