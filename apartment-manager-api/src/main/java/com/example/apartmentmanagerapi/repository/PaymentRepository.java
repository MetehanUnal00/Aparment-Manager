package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Payment entity.
 * Provides database access methods for payment management including
 * custom queries for reporting and analysis.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    /**
     * Find all payments for a specific flat
     * @param flatId The flat ID
     * @return List of payments ordered by payment date descending
     */
    List<Payment> findByFlatIdOrderByPaymentDateDesc(Long flatId);
    
    /**
     * Find all payments for a specific flat with pagination
     * @param flatId The flat ID
     * @param pageable Pagination information
     * @return Page of payments
     */
    Page<Payment> findByFlatId(Long flatId, Pageable pageable);
    
    /**
     * Find payments by flat within a date range
     * @param flatId The flat ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of payments in the date range
     */
    List<Payment> findByFlatIdAndPaymentDateBetween(
            Long flatId, 
            LocalDateTime startDate, 
            LocalDateTime endDate
    );
    
    /**
     * Find payments by building within a date range
     * @param buildingId The building ID
     * @param startDate Start date (inclusive)
     * @param endDate End date (inclusive)
     * @return List of payments for all flats in the building
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN p.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "ORDER BY p.paymentDate DESC")
    List<Payment> findByBuildingIdAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Calculate total payments for a flat
     * @param flatId The flat ID
     * @return Total payment amount
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.flat.id = :flatId")
    BigDecimal calculateTotalPaymentsForFlat(@Param("flatId") Long flatId);
    
    /**
     * Calculate total payments for a building
     * @param buildingId The building ID
     * @return Total payment amount for all flats in the building
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId")
    BigDecimal calculateTotalPaymentsForBuilding(@Param("buildingId") Long buildingId);
    
    /**
     * Find payments by payment method
     * @param paymentMethod The payment method
     * @param pageable Pagination information
     * @return Page of payments
     */
    Page<Payment> findByPaymentMethod(Payment.PaymentMethod paymentMethod, Pageable pageable);
    
    /**
     * Find payments recorded by a specific user
     * @param userId The user ID who recorded the payments
     * @param pageable Pagination information
     * @return Page of payments
     */
    Page<Payment> findByRecordedById(Long userId, Pageable pageable);
    
    /**
     * Check if a payment with reference number already exists
     * Useful for preventing duplicate payments
     * @param referenceNumber The reference number to check
     * @return true if exists
     */
    boolean existsByReferenceNumber(String referenceNumber);
    
    /**
     * Find payment by reference number
     * @param referenceNumber The reference number
     * @return Optional containing the payment if found
     */
    Optional<Payment> findByReferenceNumber(String referenceNumber);
    
    /**
     * Get payment statistics for a building
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return Payment statistics including count and total amount
     */
    @Query("SELECT COUNT(p), COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate")
    List<Object[]> getPaymentStatistics(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find payments by building within a date range (using LocalDate)
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return List of payments
     */
    @Query("SELECT p FROM Payment p " +
           "JOIN p.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND DATE(p.paymentDate) >= :startDate " +
           "AND DATE(p.paymentDate) <= :endDate " +
           "ORDER BY p.paymentDate DESC")
    List<Payment> findByBuildingAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
    
    /**
     * Get total payments by building and date range
     * @param buildingId The building ID
     * @param startDate Start date
     * @param endDate End date
     * @return Total payment amount
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "JOIN p.flat f " +
           "WHERE f.apartmentBuilding.id = :buildingId " +
           "AND DATE(p.paymentDate) >= :startDate " +
           "AND DATE(p.paymentDate) <= :endDate")
    BigDecimal getTotalPaymentsByBuildingAndDateRange(
            @Param("buildingId") Long buildingId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}