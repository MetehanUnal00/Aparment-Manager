package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository interface for Expense entity.
 * Provides database access methods for expense tracking and reporting
 * including category-based analysis and time-period queries.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    
    /**
     * Find all expenses for a specific building
     * @param buildingId The building ID
     * @return List of expenses ordered by expense date descending
     */
    List<Expense> findByBuildingIdOrderByExpenseDateDesc(Long buildingId);
    
    /**
     * Find all expenses for a specific building with pagination
     * @param buildingId The building ID
     * @param pageable Pagination information
     * @return Page of expenses
     */
    Page<Expense> findByBuildingId(Long buildingId, Pageable pageable);
    
    /**
     * Find expenses by building and category
     * @param buildingId The building ID
     * @param category The expense category
     * @return List of expenses
     */
    List<Expense> findByBuildingIdAndCategory(Long buildingId, Expense.ExpenseCategory category);
    
    /**
     * Find expenses by building within a date range
     * @param buildingId The building ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of expenses in the date range
     */
    List<Expense> findByBuildingIdAndExpenseDateBetween(
            Long buildingId,
            LocalDate startDate,
            LocalDate endDate
    );
    
    /**
     * Find expenses by category within a date range
     * @param buildingId The building ID
     * @param category The expense category
     * @param startDate Start date
     * @param endDate End date
     * @return List of expenses
     */
    List<Expense> findByBuildingIdAndCategoryAndExpenseDateBetween(
            Long buildingId,
            Expense.ExpenseCategory category,
            LocalDate startDate,
            LocalDate endDate
    );
    
    /**
     * Calculate total expenses for a building
     * @param buildingId The building ID
     * @return Total expense amount
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e WHERE e.building.id = :buildingId")
    BigDecimal calculateTotalExpensesForBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Calculate total expenses for a building within a date range
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return Total expense amount
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalExpensesForBuildingInPeriod(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Get expense summary by category for a building
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of category and total amount pairs
     */
    @Query("SELECT e.category, COALESCE(SUM(e.amount), 0), COUNT(e) " +
           "FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND e.expenseDate BETWEEN :startDate AND :endDate " +
           "GROUP BY e.category " +
           "ORDER BY SUM(e.amount) DESC")
    List<Object[]> getExpenseSummaryByCategory(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Get monthly expense totals for a building
     * @param buildingId The building ID
     * @param year The year
     * @return List of month and total amount pairs
     */
    @Query("SELECT MONTH(e.expenseDate), COALESCE(SUM(e.amount), 0) " +
           "FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND YEAR(e.expenseDate) = :year " +
           "GROUP BY MONTH(e.expenseDate) " +
           "ORDER BY MONTH(e.expenseDate)")
    List<Object[]> getMonthlyExpenseTotals(
            @Param("buildingId") Long buildingId,
            @Param("year") int year
    );
    
    /**
     * Find recurring expenses for a building
     * @param buildingId The building ID
     * @return List of recurring expenses
     */
    List<Expense> findByBuildingIdAndIsRecurringTrue(Long buildingId);
    
    /**
     * Find expenses by vendor name
     * @param buildingId The building ID
     * @param vendorName The vendor name (partial match)
     * @return List of expenses
     */
    @Query("SELECT e FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND LOWER(e.vendorName) LIKE LOWER(CONCAT('%', :vendorName, '%'))")
    List<Expense> findByBuildingIdAndVendorNameContaining(
            @Param("buildingId") Long buildingId,
            @Param("vendorName") String vendorName
    );
    
    /**
     * Get top expense categories for a building
     * @param buildingId The building ID
     * @param limit Number of top categories to return
     * @return List of top categories with their totals
     */
    @Query(value = "SELECT e.category, COALESCE(SUM(e.amount), 0) as total " +
           "FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "GROUP BY e.category " +
           "ORDER BY total DESC " +
           "LIMIT :limit", nativeQuery = true)
    List<Object[]> getTopExpenseCategories(
            @Param("buildingId") Long buildingId,
            @Param("limit") int limit
    );
    
    /**
     * Find expenses recorded by a specific user
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of expenses
     */
    Page<Expense> findByRecordedById(Long userId, Pageable pageable);
    
    /**
     * Find expenses by building and date range
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of expenses
     */
    @Query("SELECT e FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND e.expenseDate >= :startDate " +
           "AND e.expenseDate <= :endDate " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByBuildingAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find expenses by building and category
     * @param buildingId The building ID
     * @param category The category
     * @return List of expenses
     */
    @Query("SELECT e FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND e.category = :category " +
           "ORDER BY e.expenseDate DESC")
    List<Expense> findByBuildingAndCategory(
            @Param("buildingId") Long buildingId,
            @Param("category") Expense.ExpenseCategory category
    );
    
    /**
     * Get total expenses by building and date range
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return Total expense amount
     */
    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM Expense e " +
           "WHERE e.building.id = :buildingId " +
           "AND e.expenseDate >= :startDate " +
           "AND e.expenseDate <= :endDate")
    BigDecimal getTotalExpensesByBuildingAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find all recurring expenses for a building
     * @param buildingId The building ID
     * @return List of recurring expenses
     */
    List<Expense> findByApartmentBuildingIdAndIsRecurringTrue(Long buildingId);
}