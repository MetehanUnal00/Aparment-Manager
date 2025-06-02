package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.ActiveContractInfo;
import com.example.apartmentmanagerapi.dto.FlatResponse;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for efficiently loading contract information for flats
 * Handles batch loading to prevent N+1 query problems
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ContractLoadingService {
    
    private final ContractRepository contractRepository;
    
    /**
     * Efficiently load active contracts for multiple flats
     * This method fetches all active contracts in a single query
     * 
     * @param flatIds List of flat IDs to load contracts for
     * @return Map of flat ID to active contract
     */
    public Map<Long, Contract> loadActiveContractsForFlats(List<Long> flatIds) {
        if (flatIds == null || flatIds.isEmpty()) {
            log.debug("No flat IDs provided for contract loading");
            return Collections.emptyMap();
        }
        
        log.debug("Loading active contracts for {} flats", flatIds.size());
        
        // Fetch all active contracts for the given flats in one query
        List<Contract> activeContracts = contractRepository.findActiveContractsByFlatIds(flatIds);
        
        log.debug("Found {} active contracts", activeContracts.size());
        
        // Convert to map for easy lookup
        // If multiple active contracts exist (shouldn't happen), take the first one
        Map<Long, Contract> contractMap = activeContracts.stream()
            .collect(Collectors.toMap(
                contract -> contract.getFlat().getId(),
                contract -> contract,
                (existing, replacement) -> {
                    log.warn("Multiple active contracts found for flat {}. Using contract {}", 
                        existing.getFlat().getId(), existing.getId());
                    return existing;
                }
            ));
        
        return contractMap;
    }
    
    /**
     * Convert Contract entity to ActiveContractInfo DTO
     * 
     * @param contract The contract entity
     * @return ActiveContractInfo DTO
     */
    public ActiveContractInfo mapToActiveContractInfo(Contract contract) {
        if (contract == null) {
            return null;
        }
        
        return ActiveContractInfo.builder()
            .contractId(contract.getId())
            .tenantName(contract.getTenantName())
            .tenantEmail(contract.getTenantEmail())
            .tenantContact(contract.getTenantContact())
            .monthlyRent(contract.getMonthlyRent())
            .securityDeposit(contract.getSecurityDeposit())
            .startDate(contract.getStartDate())
            .endDate(contract.getEndDate())
            .moveInDate(contract.getStartDate()) // Move-in date is same as start date
            .daysUntilExpiry(calculateDaysUntilExpiry(contract))
            .isExpiringSoon(isExpiringSoon(contract))
            .contractStatus(contract.getStatus().name())
            .outstandingBalance(calculateOutstandingBalance(contract))
            .hasOverdueDues(hasOverdueDues(contract))
            .build();
    }
    
    /**
     * Load occupancy summary for a flat
     * This includes historical contract data
     * 
     * @param flatId The flat ID
     * @return Occupancy summary
     */
    @Cacheable(value = "flatOccupancySummary", key = "#flatId")
    public FlatResponse.OccupancySummary loadOccupancySummary(Long flatId) {
        log.debug("Loading occupancy summary for flat {}", flatId);
        
        // Get all contracts for the flat (not just active ones)
        List<Contract> allContracts = contractRepository.findByFlatIdOrderByStartDateDesc(flatId);
        
        if (allContracts.isEmpty()) {
            return FlatResponse.OccupancySummary.builder()
                .totalContracts(0)
                .totalMonthsOccupied(0)
                .averageRent(BigDecimal.ZERO)
                .build();
        }
        
        // Calculate summary statistics
        LocalDate firstOccupancyDate = allContracts.stream()
            .map(Contract::getStartDate)
            .min(LocalDate::compareTo)
            .orElse(null);
        
        LocalDate lastVacancyDate = findLastVacancyDate(allContracts);
        
        BigDecimal totalRent = allContracts.stream()
            .map(Contract::getMonthlyRent)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal averageRent = totalRent.divide(
            BigDecimal.valueOf(allContracts.size()), 
            2, 
            BigDecimal.ROUND_HALF_UP
        );
        
        int totalMonthsOccupied = allContracts.stream()
            .mapToInt(this::calculateContractMonths)
            .sum();
        
        return FlatResponse.OccupancySummary.builder()
            .totalContracts(allContracts.size())
            .firstOccupancyDate(firstOccupancyDate)
            .lastVacancyDate(lastVacancyDate)
            .averageRent(averageRent)
            .totalMonthsOccupied(totalMonthsOccupied)
            .build();
    }
    
    /**
     * Calculate days until contract expiry
     */
    private Integer calculateDaysUntilExpiry(Contract contract) {
        if (contract.getEndDate() == null || contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            return null;
        }
        
        long days = ChronoUnit.DAYS.between(LocalDate.now(), contract.getEndDate());
        return days >= 0 ? (int) days : null;
    }
    
    /**
     * Check if contract is expiring soon (within 30 days)
     */
    private boolean isExpiringSoon(Contract contract) {
        Integer daysUntilExpiry = calculateDaysUntilExpiry(contract);
        return daysUntilExpiry != null && daysUntilExpiry <= 30;
    }
    
    /**
     * Calculate outstanding balance from monthly dues
     */
    private BigDecimal calculateOutstandingBalance(Contract contract) {
        if (contract.getMonthlyDues() == null || contract.getMonthlyDues().isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        return contract.getMonthlyDues().stream()
            .filter(due -> due.getStatus() == com.example.apartmentmanagerapi.entity.MonthlyDue.DueStatus.UNPAID)
            .map(com.example.apartmentmanagerapi.entity.MonthlyDue::getDueAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    /**
     * Check if contract has overdue dues
     */
    private boolean hasOverdueDues(Contract contract) {
        if (contract.getMonthlyDues() == null || contract.getMonthlyDues().isEmpty()) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        return contract.getMonthlyDues().stream()
            .anyMatch(due -> 
                due.getStatus() == com.example.apartmentmanagerapi.entity.MonthlyDue.DueStatus.UNPAID &&
                due.getDueDate() != null &&
                due.getDueDate().isBefore(today)
            );
    }
    
    /**
     * Calculate number of months in a contract
     */
    private int calculateContractMonths(Contract contract) {
        if (contract.getStartDate() == null || contract.getEndDate() == null) {
            return 0;
        }
        
        return (int) ChronoUnit.MONTHS.between(contract.getStartDate(), contract.getEndDate()) + 1;
    }
    
    /**
     * Find the last vacancy date based on contract gaps
     */
    private LocalDate findLastVacancyDate(List<Contract> contracts) {
        if (contracts.size() <= 1) {
            return null;
        }
        
        LocalDate lastVacancy = null;
        
        for (int i = 0; i < contracts.size() - 1; i++) {
            Contract current = contracts.get(i);
            Contract next = contracts.get(i + 1);
            
            // Check if there's a gap between contracts
            if (current.getEndDate() != null && next.getStartDate() != null &&
                current.getEndDate().isBefore(next.getStartDate().minusDays(1))) {
                lastVacancy = current.getEndDate().plusDays(1);
            }
        }
        
        return lastVacancy;
    }
}