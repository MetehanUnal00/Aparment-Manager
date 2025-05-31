package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.MonthlyDue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing monthly dues.
 * Handles due generation, debtor tracking, and collection analytics.
 */
public interface IMonthlyDueService {

    /**
     * Generates monthly dues for all active flats in a building.
     * Creates due entries with specified amount and due date.
     * 
     * @param buildingId the building ID
     * @param dueAmount the amount due per flat
     * @param dueDate the due date for payment
     * @param description optional description for the dues
     * @return list of created monthly due entries
     */
    List<MonthlyDue> generateMonthlyDuesForBuilding(Long buildingId, BigDecimal dueAmount, LocalDate dueDate, String description);

    /**
     * Automatically generates monthly dues for all buildings.
     * Scheduled to run on the 1st of each month.
     */
    void generateMonthlyDuesAutomatically();

    /**
     * Updates the status of overdue monthly dues.
     * Scheduled to run daily to identify overdue payments.
     */
    void updateOverdueStatuses();

    /**
     * Retrieves all debtors (flats with unpaid dues) in a building.
     * 
     * @param buildingId the building ID
     * @return list of flats with unpaid dues
     */
    List<Flat> getDebtorsByBuilding(Long buildingId);

    /**
     * Gets detailed debtor information for a building.
     * Maps each debtor flat to their total outstanding amount.
     * 
     * @param buildingId the building ID
     * @return map of flats to their total debt amounts
     */
    Map<Flat, BigDecimal> getDebtorDetailsForBuilding(Long buildingId);

    /**
     * Calculates the total debt for a specific flat.
     * 
     * @param flatId the flat ID
     * @return total outstanding debt amount
     */
    BigDecimal calculateTotalDebt(Long flatId);

    /**
     * Retrieves all monthly dues for a specific flat.
     * 
     * @param flatId the flat ID
     * @return list of monthly dues ordered by due date
     */
    List<MonthlyDue> getMonthlyDuesByFlat(Long flatId);

    /**
     * Retrieves all overdue dues for a building.
     * 
     * @param buildingId the building ID
     * @return list of overdue monthly dues
     */
    List<MonthlyDue> getOverdueDuesByBuilding(Long buildingId);

    /**
     * Creates a new monthly due entry.
     * 
     * @param monthlyDue the monthly due entity to create
     * @return the created monthly due
     */
    MonthlyDue createMonthlyDue(MonthlyDue monthlyDue);

    /**
     * Updates an existing monthly due.
     * 
     * @param monthlyDue the monthly due entity with updates
     * @return the updated monthly due
     */
    MonthlyDue updateMonthlyDue(MonthlyDue monthlyDue);

    /**
     * Cancels a monthly due entry.
     * Sets status to CANCELLED for audit trail.
     * 
     * @param monthlyDueId the monthly due ID to cancel
     */
    void cancelMonthlyDue(Long monthlyDueId);

    /**
     * Calculates the collection rate for a building in a date range.
     * 
     * @param buildingId the building ID
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     * @return collection rate as a percentage (0-100)
     */
    double getCollectionRate(Long buildingId, LocalDate startDate, LocalDate endDate);
}