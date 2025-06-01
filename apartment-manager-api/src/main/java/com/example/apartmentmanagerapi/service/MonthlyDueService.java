package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.ApartmentBuilding;
import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.event.MonthlyDuesGeneratedEvent;
import com.example.apartmentmanagerapi.repository.ApartmentBuildingRepository;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service class for managing monthly dues for apartment flats.
 * Handles due generation, overdue tracking, and debtor management.
 * Includes scheduled tasks for automatic due generation and status updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MonthlyDueService implements IMonthlyDueService {
    
    private final MonthlyDueRepository monthlyDueRepository;
    private final FlatRepository flatRepository;
    private final ApartmentBuildingRepository apartmentBuildingRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Generates monthly dues for all active flats in a building.
     * Ensures idempotency through unique constraint on (flat_id, due_date).
     * 
     * @param buildingId ID of the building
     * @param dueAmount Amount to charge each flat
     * @param dueDate Due date for the monthly charge
     * @param description Description of the charge
     * @return List of created monthly dues
     */
    public List<MonthlyDue> generateMonthlyDuesForBuilding(
            Long buildingId, BigDecimal dueAmount, LocalDate dueDate, String description) {
        
        log.info("Generating monthly dues for building ID: {} with amount: {} for date: {}", 
                buildingId, dueAmount, dueDate);
        
        // Validate building exists
        ApartmentBuilding building = apartmentBuildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Building not found with ID: " + buildingId));
        
        // Get all active flats in the building
        List<Flat> activeFlats = flatRepository.findByApartmentBuildingIdAndIsActiveTrue(buildingId);
        
        List<MonthlyDue> createdDues = new ArrayList<>();
        int skippedCount = 0;
        
        for (Flat flat : activeFlats) {
            try {
                MonthlyDue monthlyDue = MonthlyDue.builder()
                        .flat(flat)
                        .dueAmount(dueAmount)
                        .dueDate(dueDate)
                        .dueDescription(description)
                        .status(MonthlyDue.DueStatus.UNPAID)
                        .paidAmount(BigDecimal.ZERO)
                        .build();
                
                MonthlyDue savedDue = monthlyDueRepository.save(monthlyDue);
                createdDues.add(savedDue);
                
            } catch (DataIntegrityViolationException e) {
                // Due already exists for this flat and date (idempotency)
                log.debug("Monthly due already exists for flat ID: {} and date: {}", 
                        flat.getId(), dueDate);
                skippedCount++;
            }
        }
        
        log.info("Generated {} monthly dues for building ID: {} ({} skipped as duplicates)", 
                createdDues.size(), buildingId, skippedCount);
        
        // Publish event if dues were generated
        if (!createdDues.isEmpty()) {
            MonthlyDuesGeneratedEvent event = new MonthlyDuesGeneratedEvent(
                this,
                buildingId,
                dueDate.getYear(),
                dueDate.getMonthValue(),
                createdDues.size(),
                dueDate
            );
            eventPublisher.publishEvent(event);
            log.debug("Published MonthlyDuesGeneratedEvent for building {}", buildingId);
        }
        
        return createdDues;
    }
    
    /**
     * Enhanced method that generates monthly dues with support for using each flat's monthly rent.
     * Maintains backward compatibility through the original method above.
     * 
     * @param buildingId ID of the building
     * @param dueAmount Amount to charge each flat (or ultimate fallback in rent-based mode)
     * @param dueDate Due date for the monthly charge
     * @param description Description of the charge
     * @param useFlatsMonthlyRent Whether to use each flat's monthlyRent instead of uniform amount
     * @param fallbackAmount Primary fallback for flats without rent (only used when useFlatsMonthlyRent=true)
     * @return List of created monthly dues
     */
    @Transactional
    @CacheEvict(value = {"debtorList", "buildingStatistics", "buildingFinancials"}, allEntries = true)
    public List<MonthlyDue> generateMonthlyDuesForBuilding(
            Long buildingId, 
            BigDecimal dueAmount,
            LocalDate dueDate, 
            String description,
            boolean useFlatsMonthlyRent,
            BigDecimal fallbackAmount) {
        
        log.info("Generating monthly dues for building ID: {} | Mode: {} | Date: {} | " +
                "DueAmount: {} | FallbackAmount: {}", 
                buildingId, 
                useFlatsMonthlyRent ? "FLAT_RENT_BASED" : "UNIFORM", 
                dueDate,
                dueAmount,
                fallbackAmount);
        
        // Validate building exists
        ApartmentBuilding building = apartmentBuildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Building not found with ID: " + buildingId));
        
        // Get all active flats in the building
        List<Flat> activeFlats = flatRepository.findByApartmentBuildingIdAndIsActiveTrue(buildingId);
        
        if (activeFlats.isEmpty()) {
            log.warn("No active flats found for building ID: {}", buildingId);
            return Collections.emptyList();
        }
        
        List<MonthlyDue> createdDues = new ArrayList<>();
        int skippedCount = 0;
        int noRentCount = 0;
        int usingFallbackCount = 0;
        
        for (Flat flat : activeFlats) {
            try {
                // Determine amount based on generation mode
                BigDecimal amountToUse = determineAmountForFlat(
                    flat, 
                    useFlatsMonthlyRent, 
                    dueAmount, 
                    fallbackAmount
                );
                
                // Track statistics for rent-based mode
                if (useFlatsMonthlyRent && 
                    (flat.getMonthlyRent() == null || 
                     flat.getMonthlyRent().compareTo(BigDecimal.ZERO) <= 0)) {
                    noRentCount++;
                    if (fallbackAmount != null && fallbackAmount.compareTo(BigDecimal.ZERO) > 0) {
                        usingFallbackCount++;
                    }
                    log.debug("Flat {} has no rent set, using {} amount: {}", 
                             flat.getFlatNumber(),
                             usingFallbackCount == noRentCount ? "fallback" : "ultimate fallback",
                             amountToUse);
                }
                
                MonthlyDue monthlyDue = MonthlyDue.builder()
                        .flat(flat)
                        .dueAmount(amountToUse)
                        .dueDate(dueDate)
                        .dueDescription(description)
                        .status(MonthlyDue.DueStatus.UNPAID)
                        .paidAmount(BigDecimal.ZERO)
                        .build();
                
                MonthlyDue savedDue = monthlyDueRepository.save(monthlyDue);
                createdDues.add(savedDue);
                
            } catch (DataIntegrityViolationException e) {
                // Due already exists for this flat and date (idempotency)
                log.debug("Monthly due already exists for flat ID: {} and date: {}", 
                        flat.getId(), dueDate);
                skippedCount++;
            }
        }
        
        log.info("Generated {} monthly dues for building ID: {} | Mode: {} | " +
                "Skipped: {} (duplicates) | No rent: {} | Using fallback: {} | Using ultimate fallback: {}", 
                createdDues.size(), 
                buildingId, 
                useFlatsMonthlyRent ? "FLAT_RENT_BASED" : "UNIFORM",
                skippedCount, 
                noRentCount,
                usingFallbackCount,
                noRentCount - usingFallbackCount);
        
        // Publish event if dues were generated
        if (!createdDues.isEmpty()) {
            MonthlyDuesGeneratedEvent event = new MonthlyDuesGeneratedEvent(
                this,
                buildingId,
                dueDate.getYear(),
                dueDate.getMonthValue(),
                createdDues.size(),
                dueDate
            );
            eventPublisher.publishEvent(event);
            log.debug("Published MonthlyDuesGeneratedEvent for building {}", buildingId);
        }
        
        return createdDues;
    }
    
    /**
     * Determines the amount to charge for a specific flat based on generation mode.
     * 
     * @param flat The flat to determine amount for
     * @param useFlatsMonthlyRent Whether to use flat's monthly rent
     * @param dueAmount The uniform/ultimate fallback amount
     * @param fallbackAmount The primary fallback amount (for rent-based mode)
     * @return The determined amount to charge
     */
    private BigDecimal determineAmountForFlat(
            Flat flat, 
            boolean useFlatsMonthlyRent,
            BigDecimal dueAmount, 
            BigDecimal fallbackAmount) {
        
        if (!useFlatsMonthlyRent) {
            // Uniform mode - simple case
            return dueAmount;
        }
        
        // Rent-based mode - check flat's monthly rent first
        if (flat.getMonthlyRent() != null && 
            flat.getMonthlyRent().compareTo(BigDecimal.ZERO) > 0) {
            return flat.getMonthlyRent();
        }
        
        // Flat has no valid rent - use fallback chain
        if (fallbackAmount != null && fallbackAmount.compareTo(BigDecimal.ZERO) > 0) {
            return fallbackAmount;
        }
        
        // Ultimate fallback to dueAmount (which was validated to exist)
        return dueAmount;
    }
    
    /**
     * Scheduled task to generate monthly dues automatically.
     * Runs on the 1st of each month at 00:00.
     * Generates dues for all buildings with auto-generation enabled.
     */
    @Scheduled(cron = "0 0 0 1 * *") // Run at 00:00 on the 1st day of every month
    public void generateMonthlyDuesAutomatically() {
        log.info("Starting automatic monthly due generation for {}", YearMonth.now());
        
        // Get all buildings with auto-generation enabled
        // For MVP, we'll generate for all active buildings
        List<ApartmentBuilding> buildings = apartmentBuildingRepository.findAll();
        
        LocalDate dueDate = LocalDate.now().withDayOfMonth(15); // Due on 15th of current month
        
        for (ApartmentBuilding building : buildings) {
            try {
                // Check if building has a default monthly fee set
                if (building.getDefaultMonthlyFee() != null && 
                    building.getDefaultMonthlyFee().compareTo(BigDecimal.ZERO) > 0) {
                    
                    generateMonthlyDuesForBuilding(
                            building.getId(),
                            building.getDefaultMonthlyFee(),
                            dueDate,
                            "Monthly maintenance fee for " + YearMonth.now()
                    );
                }
            } catch (Exception e) {
                log.error("Failed to generate monthly dues for building ID: {}", 
                        building.getId(), e);
                // Continue with other buildings
            }
        }
        
        log.info("Completed automatic monthly due generation");
    }
    
    /**
     * Scheduled task to update overdue statuses.
     * Runs daily at 01:00 to mark unpaid dues as overdue.
     */
    @Scheduled(cron = "0 0 1 * * *") // Run daily at 01:00
    public void updateOverdueStatuses() {
        log.info("Starting overdue status update check");
        
        LocalDate today = LocalDate.now();
        
        // Find all unpaid dues with due date before today
        List<MonthlyDue> overdueCandidates = monthlyDueRepository
                .findByStatusAndDueDateBefore(MonthlyDue.DueStatus.UNPAID, today);
        
        int updatedCount = 0;
        for (MonthlyDue due : overdueCandidates) {
            due.setStatus(MonthlyDue.DueStatus.OVERDUE);
            monthlyDueRepository.save(due);
            updatedCount++;
        }
        
        log.info("Updated {} monthly dues to OVERDUE status", updatedCount);
    }
    
    /**
     * Gets list of debtors (flats with overdue payments) for a building.
     * 
     * @param buildingId ID of the building
     * @return List of flats with overdue payments
     */
    @Transactional(readOnly = true)
    public List<Flat> getDebtorsByBuilding(Long buildingId) {
        log.debug("Retrieving debtors for building ID: {}", buildingId);
        return monthlyDueRepository.findFlatsWithOverdueDues(buildingId);
    }
    
    /**
     * Gets detailed debtor information including total debt amount.
     * 
     * @param buildingId ID of the building
     * @return Map of flat to total debt amount
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "debtorList", key = "#buildingId")
    public Map<Flat, BigDecimal> getDebtorDetailsForBuilding(Long buildingId) {
        log.debug("Retrieving detailed debtor information for building ID: {}", buildingId);
        
        List<Flat> debtors = getDebtorsByBuilding(buildingId);
        
        return debtors.stream()
                .collect(Collectors.toMap(
                        flat -> flat,
                        flat -> calculateTotalDebt(flat.getId())
                ));
    }
    
    /**
     * Calculates total unpaid/overdue amount for a flat.
     * 
     * @param flatId ID of the flat
     * @return Total debt amount
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "flatBalance", key = "'debt-' + #flatId")
    public BigDecimal calculateTotalDebt(Long flatId) {
        BigDecimal totalUnpaid = monthlyDueRepository.getTotalUnpaidDuesByFlat(flatId);
        return totalUnpaid != null ? totalUnpaid : BigDecimal.ZERO;
    }
    
    /**
     * Gets monthly due history for a specific flat.
     * 
     * @param flatId ID of the flat
     * @return List of monthly dues ordered by due date descending
     */
    @Transactional(readOnly = true)
    public List<MonthlyDue> getMonthlyDuesByFlat(Long flatId) {
        log.debug("Retrieving monthly dues for flat ID: {}", flatId);
        return monthlyDueRepository.findByFlatIdOrderByDueDateDesc(flatId);
    }
    
    /**
     * Gets overdue monthly dues for a building.
     * 
     * @param buildingId ID of the building
     * @return List of overdue monthly dues
     */
    @Transactional(readOnly = true)
    public List<MonthlyDue> getOverdueDuesByBuilding(Long buildingId) {
        log.debug("Retrieving overdue dues for building ID: {}", buildingId);
        return monthlyDueRepository.findOverdueDuesByBuilding(
                buildingId, MonthlyDue.DueStatus.OVERDUE);
    }
    
    /**
     * Manually creates a single monthly due for a flat.
     * Used for custom charges or adjustments.
     * 
     * @param monthlyDue Monthly due entity to create
     * @return Created monthly due
     */
    @Caching(evict = {
        @CacheEvict(value = "debtorList", allEntries = true),
        @CacheEvict(value = "flatBalance", key = "'debt-' + #monthlyDue.flat.id")
    })
    public MonthlyDue createMonthlyDue(MonthlyDue monthlyDue) {
        log.info("Creating manual monthly due for flat ID: {} with amount: {}", 
                monthlyDue.getFlat().getId(), monthlyDue.getDueAmount());
        
        // Validate flat exists
        Flat flat = flatRepository.findById(monthlyDue.getFlat().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flat not found with ID: " + monthlyDue.getFlat().getId()));
        
        monthlyDue.setFlat(flat);
        
        // Set default values if not provided
        if (monthlyDue.getStatus() == null) {
            monthlyDue.setStatus(MonthlyDue.DueStatus.UNPAID);
        }
        if (monthlyDue.getPaidAmount() == null) {
            monthlyDue.setPaidAmount(BigDecimal.ZERO);
        }
        
        return monthlyDueRepository.save(monthlyDue);
    }
    
    /**
     * Updates a monthly due (e.g., for corrections or manual adjustments).
     * 
     * @param monthlyDue Monthly due with updates
     * @return Updated monthly due
     */
    @CacheEvict(value = {"debtorList", "flatBalance"}, allEntries = true)
    public MonthlyDue updateMonthlyDue(MonthlyDue monthlyDue) {
        log.info("Updating monthly due ID: {}", monthlyDue.getId());
        
        MonthlyDue existingDue = monthlyDueRepository.findById(monthlyDue.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Monthly due not found with ID: " + monthlyDue.getId()));
        
        // Update allowed fields
        existingDue.setDueAmount(monthlyDue.getDueAmount());
        existingDue.setDueDescription(monthlyDue.getDueDescription());
        existingDue.setStatus(monthlyDue.getStatus());
        existingDue.setPaidAmount(monthlyDue.getPaidAmount());
        existingDue.setPaymentDate(monthlyDue.getPaymentDate());
        
        return monthlyDueRepository.save(existingDue);
    }
    
    /**
     * Cancels a monthly due (sets status to CANCELLED).
     * 
     * @param monthlyDueId ID of the monthly due to cancel
     */
    public void cancelMonthlyDue(Long monthlyDueId) {
        log.info("Cancelling monthly due ID: {}", monthlyDueId);
        
        MonthlyDue monthlyDue = monthlyDueRepository.findById(monthlyDueId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Monthly due not found with ID: " + monthlyDueId));
        
        if (monthlyDue.getStatus() == MonthlyDue.DueStatus.PAID) {
            throw new IllegalStateException(
                    "Cannot cancel a paid monthly due. ID: " + monthlyDueId);
        }
        
        monthlyDue.setStatus(MonthlyDue.DueStatus.CANCELLED);
        monthlyDueRepository.save(monthlyDue);
        
        log.info("Monthly due ID: {} cancelled successfully", monthlyDueId);
    }
    
    /**
     * Gets collection rate statistics for a building.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date for statistics
     * @param endDate End date for statistics
     * @return Collection rate as percentage (0-100)
     */
    @Transactional(readOnly = true)
    public double getCollectionRate(Long buildingId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating collection rate for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        
        // Get all monthly dues for the period
        List<MonthlyDue> dues = monthlyDueRepository
                .findByBuildingAndDateRange(buildingId, startDate, endDate);
        
        if (dues.isEmpty()) {
            return 100.0; // No dues means 100% collection
        }
        
        long paidCount = dues.stream()
                .filter(due -> due.getStatus() == MonthlyDue.DueStatus.PAID)
                .count();
        
        return (double) paidCount / dues.size() * 100.0;
    }

    /**
     * Retrieves all monthly dues for a building.
     * Returns all dues regardless of status, sorted by flat number and due date.
     * 
     * @param buildingId the building ID
     * @return list of all monthly dues for the building
     */
    @Transactional(readOnly = true)
    @Override
    public List<MonthlyDue> getAllMonthlyDuesForBuilding(Long buildingId) {
        log.info("Retrieving all monthly dues for building ID: {}", buildingId);
        
        // Use a wide date range to get all dues
        LocalDate startDate = LocalDate.of(2020, 1, 1); // Far past date
        LocalDate endDate = LocalDate.now().plusYears(1); // Future date
        
        return monthlyDueRepository.findByBuildingAndDateRange(buildingId, startDate, endDate);
    }
}