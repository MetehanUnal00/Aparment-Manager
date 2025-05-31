package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service interface for managing apartment payments and transactions.
 * Handles payment creation, balance updates, and transaction history.
 * Ensures data consistency with transactional operations.
 */
public interface IPaymentService {

    /**
     * Creates a new payment for a flat.
     * Updates related monthly dues and handles optimistic locking.
     * 
     * @param payment Payment entity to save
     * @return Saved payment entity
     * @throws IllegalArgumentException if flat doesn't exist
     * @throws IllegalStateException if payment amount exceeds outstanding balance
     */
    Payment createPayment(Payment payment);

    /**
     * Retrieves payment history for a specific flat.
     * 
     * @param flatId ID of the flat
     * @return List of payments ordered by payment date descending
     */
    List<Payment> getPaymentsByFlat(Long flatId);

    /**
     * Retrieves payments for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of payments within the date range
     */
    List<Payment> getPaymentsByBuildingAndDateRange(Long buildingId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates total payment amount for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return Total payment amount
     */
    BigDecimal getTotalPaymentsByBuildingAndDateRange(Long buildingId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates outstanding balance for a flat.
     * Sum of unpaid monthly dues minus any unallocated payments.
     * 
     * @param flatId ID of the flat
     * @return Outstanding balance amount
     */
    BigDecimal calculateOutstandingBalance(Long flatId);

    /**
     * Updates a payment (e.g., for corrections).
     * Handles optimistic locking via @Version field.
     * 
     * @param payment Payment entity with updates
     * @return Updated payment entity
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if concurrent update detected
     */
    Payment updatePayment(Payment payment);

    /**
     * Deletes a payment (soft delete recommended for audit trail).
     * In production, consider implementing soft delete instead.
     * 
     * @param paymentId ID of the payment to delete
     */
    void deletePayment(Long paymentId);
}