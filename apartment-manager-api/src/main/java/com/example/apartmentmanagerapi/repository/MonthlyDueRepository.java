package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for MonthlyDue entity.
 * Provides database access methods for monthly due management including
 * methods for due generation, status tracking, and overdue management.
 */
@Repository
public interface MonthlyDueRepository extends JpaRepository<MonthlyDue, Long> {
    
    /**
     * Find all monthly dues for a specific flat
     * @param flatId The flat ID
     * @return List of monthly dues ordered by due date descending
     */
    List<MonthlyDue> findByFlatIdOrderByDueDateDesc(Long flatId);
    
    /**
     * Find all monthly dues for a specific flat with pagination
     * @param flatId The flat ID
     * @param pageable Pagination information
     * @return Page of monthly dues
     */
    Page<MonthlyDue> findByFlatId(Long flatId, Pageable pageable);
    
    /**
     * Find monthly due for a specific flat and due date
     * Used to check if a due already exists (idempotency)
     * @param flatId The flat ID
     * @param dueDate The due date
     * @return Optional containing the monthly due if found
     */
    Optional<MonthlyDue> findByFlatIdAndDueDate(Long flatId, LocalDate dueDate);
    
    /**
     * Check if a monthly due exists for a flat and date
     * @param flatId The flat ID
     * @param dueDate The due date
     * @return true if exists
     */
    boolean existsByFlatIdAndDueDate(Long flatId, LocalDate dueDate);
    
    /**
     * Find all unpaid dues for a flat
     * @param flatId The flat ID
     * @return List of unpaid dues
     */
    List<MonthlyDue> findByFlatIdAndStatus(Long flatId, MonthlyDue.DueStatus status);
    
    /**
     * Find all overdue payments across all flats
     * @param currentDate The current date to compare against
     * @return List of overdue monthly dues
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "WHERE md.status = 'UNPAID' " +
           "AND md.dueDate < :currentDate " +
           "ORDER BY md.dueDate ASC")
    List<MonthlyDue> findAllOverdue(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find overdue payments for a specific building
     * @param buildingId The building ID
     * @param currentDate The current date
     * @return List of overdue dues for the building
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "JOIN md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.status = 'UNPAID' " +
           "AND md.dueDate < :currentDate " +
           "ORDER BY f.flatNumber, md.dueDate")
    List<MonthlyDue> findOverdueByBuilding(
            @Param("buildingId") Long buildingId,
            @Param("currentDate") LocalDate currentDate
    );
    
    /**
     * Calculate total unpaid dues for a flat
     * @param flatId The flat ID
     * @return Total unpaid amount
     */
    @Query("SELECT COALESCE(SUM(md.dueAmount), 0) FROM MonthlyDue md " +
           "WHERE md.flat.id = :flatId AND md.status = 'UNPAID'")
    BigDecimal calculateTotalUnpaidForFlat(@Param("flatId") Long flatId);
    
    /**
     * Calculate total unpaid dues for a building
     * @param buildingId The building ID
     * @return Total unpaid amount for all flats
     */
    @Query("SELECT COALESCE(SUM(md.dueAmount), 0) FROM MonthlyDue md " +
           "JOIN md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.status = 'UNPAID'")
    BigDecimal calculateTotalUnpaidForBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Update overdue status for all unpaid dues past their due date
     * This can be called by a scheduled task
     * @param currentDate The current date
     * @return Number of records updated
     */
    @Modifying
    @Query("UPDATE MonthlyDue md " +
           "SET md.status = 'OVERDUE' " +
           "WHERE md.status = 'UNPAID' " +
           "AND md.dueDate < :currentDate")
    int markOverdueDues(@Param("currentDate") LocalDate currentDate);
    
    /**
     * Find dues for a specific month across all flats in a building
     * @param buildingId The building ID
     * @param year The year
     * @param month The month (1-12)
     * @return List of monthly dues
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "JOIN md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND YEAR(md.dueDate) = :year " +
           "AND MONTH(md.dueDate) = :month " +
           "ORDER BY f.flatNumber")
    List<MonthlyDue> findByBuildingAndMonth(
            @Param("buildingId") Long buildingId,
            @Param("year") int year,
            @Param("month") int month
    );
    
    /**
     * Get debtor list for a building (flats with unpaid dues)
     * @param buildingId The building ID
     * @return List of flats with unpaid dues and their amounts
     */
    @Query("SELECT f.id, f.flatNumber, f.tenantName, " +
           "COUNT(md), COALESCE(SUM(md.dueAmount), 0) " +
           "FROM MonthlyDue md " +
           "JOIN md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.status IN ('UNPAID', 'OVERDUE') " +
           "GROUP BY f.id, f.flatNumber, f.tenantName " +
           "ORDER BY SUM(md.dueAmount) DESC")
    List<Object[]> getDebtorListForBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Count unpaid dues for a flat
     * @param flatId The flat ID
     * @return Number of unpaid dues
     */
    long countByFlatIdAndStatusIn(Long flatId, List<MonthlyDue.DueStatus> statuses);
    
    /**
     * Find unpaid dues for a flat ordered by due date (oldest first)
     * @param flatId The flat ID
     * @return List of unpaid dues
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "WHERE md.flat.id = :flatId " +
           "AND md.status IN ('UNPAID', 'OVERDUE') " +
           "ORDER BY md.dueDate ASC")
    List<MonthlyDue> findUnpaidDuesByFlatOrderByDueDate(@Param("flatId") Long flatId);
    
    /**
     * Get total unpaid dues amount for a flat
     * @param flatId The flat ID
     * @return Total unpaid amount
     */
    @Query("SELECT COALESCE(SUM(md.dueAmount - md.paidAmount), 0) FROM MonthlyDue md " +
           "WHERE md.flat.id = :flatId " +
           "AND md.status IN ('UNPAID', 'OVERDUE', 'PARTIALLY_PAID')")
    BigDecimal getTotalUnpaidDuesByFlat(@Param("flatId") Long flatId);
    
    /**
     * Find dues by flat and payment date
     * @param flatId The flat ID
     * @param paymentDate The payment date
     * @return List of monthly dues
     */
    List<MonthlyDue> findByFlatIdAndPaymentDate(Long flatId, java.time.LocalDateTime paymentDate);
    
    /**
     * Find dues by status and due date before
     * @param status The status
     * @param date The date
     * @return List of monthly dues
     */
    List<MonthlyDue> findByStatusAndDueDateBefore(MonthlyDue.DueStatus status, LocalDate date);
    
    /**
     * Find flats with overdue dues for a building
     * @param buildingId The building ID
     * @return List of flats with overdue payments
     */
    @Query("SELECT DISTINCT f FROM Flat f " +
           "JOIN f.monthlyDues md " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.status = 'OVERDUE'")
    List<com.example.apartmentmanagerapi.entity.Flat> findFlatsWithOverdueDues(@Param("buildingId") Long buildingId);
    
    /**
     * Find overdue dues by building and status
     * @param buildingId The building ID
     * @param status The status
     * @return List of overdue dues
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "JOIN md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.status = :status " +
           "ORDER BY md.dueDate ASC")
    List<MonthlyDue> findOverdueDuesByBuilding(
            @Param("buildingId") Long buildingId,
            @Param("status") MonthlyDue.DueStatus status
    );
    
    /**
     * Find dues by building and date range
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of monthly dues
     */
    @Query("SELECT md FROM MonthlyDue md " +
           "JOIN FETCH md.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND md.dueDate >= :startDate " +
           "AND md.dueDate <= :endDate " +
           "ORDER BY md.dueDate")
    List<MonthlyDue> findByBuildingAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Find all monthly dues for a specific contract
     * @param contract The contract
     * @return List of monthly dues for the contract
     */
    List<MonthlyDue> findByContract(Contract contract);
    
    /**
     * Find all monthly dues for a specific contract by ID
     * @param contractId The contract ID
     * @return List of monthly dues for the contract
     */
    @Query("SELECT md FROM MonthlyDue md WHERE md.contract.id = :contractId ORDER BY md.dueDate")
    List<MonthlyDue> findByContractId(@Param("contractId") Long contractId);
    
    /**
     * Find monthly dues by contract and status
     * @param contract The contract
     * @param statuses List of statuses to filter by
     * @return List of monthly dues matching the criteria
     */
    List<MonthlyDue> findByContractAndStatusIn(Contract contract, List<MonthlyDue.DueStatus> statuses);
}