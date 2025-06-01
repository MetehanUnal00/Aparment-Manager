package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.*;
import com.example.apartmentmanagerapi.entity.Contract;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for contract management
 */
public interface IContractService {
    
    /**
     * Create a new contract
     * @param request Contract creation request
     * @return Created contract response
     */
    ContractResponse createContract(ContractRequest request);
    
    /**
     * Get contract by ID
     * @param contractId Contract ID
     * @return Contract response
     */
    ContractResponse getContractById(Long contractId);
    
    /**
     * Get active contract for a flat
     * @param flatId Flat ID
     * @return Active contract response or null
     */
    ContractResponse getActiveContractByFlatId(Long flatId);
    
    /**
     * Get all contracts for a flat
     * @param flatId Flat ID
     * @return List of contract summaries
     */
    List<ContractSummaryResponse> getContractsByFlatId(Long flatId);
    
    /**
     * Get contracts by building with pagination
     * @param buildingId Building ID
     * @param pageable Pagination parameters
     * @return Page of contract summaries
     */
    Page<ContractSummaryResponse> getContractsByBuildingId(Long buildingId, Pageable pageable);
    
    /**
     * Search contracts by tenant name
     * @param search Search term
     * @param pageable Pagination parameters
     * @return Page of contract summaries
     */
    Page<ContractSummaryResponse> searchContractsByTenantName(String search, Pageable pageable);
    
    /**
     * Renew a contract
     * @param contractId Current contract ID
     * @param request Renewal request
     * @return New contract response
     */
    ContractResponse renewContract(Long contractId, ContractRenewalRequest request);
    
    /**
     * Cancel a contract
     * @param contractId Contract ID
     * @param request Cancellation request
     * @return Updated contract response
     */
    ContractResponse cancelContract(Long contractId, ContractCancellationRequest request);
    
    /**
     * Modify a contract (creates superseding contract)
     * @param contractId Current contract ID
     * @param request Modification request
     * @return New contract response
     */
    ContractResponse modifyContract(Long contractId, ContractModificationRequest request);
    
    /**
     * Get contracts expiring within specified days
     * @param days Number of days
     * @return List of expiring contract summaries
     */
    List<ContractSummaryResponse> getExpiringContracts(int days);
    
    /**
     * Get contracts with overdue payments
     * @return List of contract summaries with overdue dues
     */
    List<ContractSummaryResponse> getContractsWithOverdueDues();
    
    /**
     * Update contract statuses based on dates
     * Called by scheduled task
     */
    void updateContractStatuses();
    
    /**
     * Get contract statistics for a building
     * @param buildingId Building ID
     * @return Statistics map
     */
    Map<String, Object> getContractStatisticsByBuilding(Long buildingId);
    
    /**
     * Validate contract dates and check for overlaps
     * @param flatId Flat ID
     * @param startDate Start date
     * @param endDate End date
     * @param excludeContractId Contract ID to exclude (for modifications)
     * @return Validation result
     */
    boolean validateContractDates(Long flatId, LocalDate startDate, LocalDate endDate, Long excludeContractId);
    
    /**
     * Get renewable contracts (expiring soon, no overdue payments)
     * @param daysAhead Days to look ahead
     * @return List of renewable contract summaries
     */
    List<ContractSummaryResponse> getRenewableContracts(int daysAhead);
    
    /**
     * Generate contract expiry notifications
     * @return List of expiry notifications
     */
    List<ContractExpiryNotification> generateExpiryNotifications();
    
    /**
     * Get total monthly rent for active contracts in a building
     * @param buildingId Building ID
     * @return Total monthly rent
     */
    BigDecimal getTotalMonthlyRentByBuilding(Long buildingId);
    
    /**
     * Check if flat has an active contract
     * @param flatId Flat ID
     * @return true if active contract exists
     */
    boolean hasActiveContract(Long flatId);
}