package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.MonthlyDue;

import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for contract-based monthly due generation
 */
public interface IContractDueGenerationService {
    
    /**
     * Generate all monthly dues for a contract
     * @param contract The contract to generate dues for
     * @return List of generated monthly dues
     */
    List<MonthlyDue> generateDuesForContract(Contract contract);
    
    /**
     * Generate monthly dues for a contract extension (renewal)
     * @param contract The renewed contract
     * @param fromDate Start generating from this date
     * @return List of generated monthly dues
     */
    List<MonthlyDue> generateDuesForContractExtension(Contract contract, LocalDate fromDate);
    
    /**
     * Cancel unpaid dues for a contract
     * @param contract The contract being cancelled
     * @return Number of dues cancelled
     */
    int cancelUnpaidDuesForContract(Contract contract);
    
    /**
     * Calculate due dates for a contract period
     * @param startDate Contract start date
     * @param endDate Contract end date
     * @param dayOfMonth Day of month when dues are due
     * @return List of calculated due dates
     */
    List<LocalDate> calculateDueDates(LocalDate startDate, LocalDate endDate, int dayOfMonth);
    
    /**
     * Adjust due date for month-end edge cases
     * @param baseDate The base date (year and month)
     * @param dayOfMonth Desired day of month
     * @return Adjusted date (last day of month if dayOfMonth exceeds month length)
     */
    LocalDate adjustDueDateForMonth(LocalDate baseDate, int dayOfMonth);
    
    /**
     * Check if dues already exist for a contract
     * @param contract The contract to check
     * @return true if dues exist
     */
    boolean duesExistForContract(Contract contract);
    
    /**
     * Get all dues for a contract
     * @param contractId Contract ID
     * @return List of monthly dues
     */
    List<MonthlyDue> getDuesByContractId(Long contractId);
    
    /**
     * Regenerate dues for a modified contract
     * @param oldContract The contract being superseded
     * @param newContract The new contract
     * @param effectiveDate Date from which to regenerate
     */
    void regenerateDuesForModifiedContract(Contract oldContract, Contract newContract, LocalDate effectiveDate);
    
    /**
     * Validate due generation parameters
     * @param contract Contract to validate
     * @return true if valid for due generation
     */
    boolean validateDueGenerationParameters(Contract contract);
}