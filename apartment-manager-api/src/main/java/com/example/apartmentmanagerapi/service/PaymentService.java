package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.Flat;
import com.example.apartmentmanagerapi.entity.Payment;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.repository.FlatRepository;
import com.example.apartmentmanagerapi.repository.PaymentRepository;
import com.example.apartmentmanagerapi.repository.MonthlyDueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Service class for managing apartment payments and transactions.
 * Handles payment creation, balance updates, and transaction history.
 * Ensures data consistency with transactional operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final FlatRepository flatRepository;
    private final MonthlyDueRepository monthlyDueRepository;
    
    /**
     * Creates a new payment for a flat.
     * Updates related monthly dues and handles optimistic locking.
     * 
     * @param payment Payment entity to save
     * @return Saved payment entity
     * @throws IllegalArgumentException if flat doesn't exist
     * @throws IllegalStateException if payment amount exceeds outstanding balance
     */
    public Payment createPayment(Payment payment) {
        log.info("Creating payment for flat ID: {} with amount: {}", 
                payment.getFlat().getId(), payment.getAmount());
        
        // Validate flat exists
        Flat flat = flatRepository.findById(payment.getFlat().getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Flat not found with ID: " + payment.getFlat().getId()));
        
        // Set the flat reference to ensure proper association
        payment.setFlat(flat);
        
        // Validate payment amount doesn't exceed outstanding balance
        BigDecimal outstandingBalance = calculateOutstandingBalance(flat.getId());
        if (payment.getAmount().compareTo(outstandingBalance) > 0) {
            throw new IllegalStateException(
                    "Payment amount exceeds outstanding balance. Outstanding: " + 
                    outstandingBalance + ", Payment: " + payment.getAmount());
        }
        
        // Save the payment
        Payment savedPayment = paymentRepository.save(payment);
        
        // Auto-allocate payment to oldest unpaid monthly dues
        allocatePaymentToDues(savedPayment);
        
        log.info("Payment created successfully with ID: {}", savedPayment.getId());
        return savedPayment;
    }
    
    /**
     * Allocates payment amount to unpaid monthly dues in chronological order.
     * Updates monthly due statuses based on payment allocation.
     * 
     * @param payment Payment to allocate
     */
    private void allocatePaymentToDues(Payment payment) {
        log.debug("Allocating payment ID: {} to monthly dues", payment.getId());
        
        // Get unpaid monthly dues for the flat, ordered by due date
        List<MonthlyDue> unpaidDues = monthlyDueRepository
                .findUnpaidDuesByFlatOrderByDueDate(payment.getFlat().getId());
        
        BigDecimal remainingAmount = payment.getAmount();
        
        for (MonthlyDue due : unpaidDues) {
            if (remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
                break; // No more payment amount to allocate
            }
            
            BigDecimal dueAmount = due.getDueAmount();
            
            if (remainingAmount.compareTo(dueAmount) >= 0) {
                // Full payment of this due
                due.setStatus(MonthlyDue.DueStatus.PAID);
                due.setPaidAmount(dueAmount);
                due.setPaymentDate(payment.getPaymentDate());
                remainingAmount = remainingAmount.subtract(dueAmount);
                
                log.debug("Fully paid monthly due ID: {} with amount: {}", 
                        due.getId(), dueAmount);
            } else {
                // Partial payment
                due.setStatus(MonthlyDue.DueStatus.PARTIALLY_PAID);
                due.setPaidAmount(due.getPaidAmount().add(remainingAmount));
                
                log.debug("Partially paid monthly due ID: {} with amount: {}", 
                        due.getId(), remainingAmount);
                
                remainingAmount = BigDecimal.ZERO;
            }
            
            monthlyDueRepository.save(due);
        }
        
        // Log if there's remaining amount (overpayment)
        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            log.warn("Payment ID: {} has overpayment amount: {}", 
                    payment.getId(), remainingAmount);
        }
    }
    
    /**
     * Retrieves payment history for a specific flat.
     * 
     * @param flatId ID of the flat
     * @return List of payments ordered by payment date descending
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByFlat(Long flatId) {
        log.debug("Retrieving payments for flat ID: {}", flatId);
        return paymentRepository.findByFlatIdOrderByPaymentDateDesc(flatId);
    }
    
    /**
     * Retrieves payments for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return List of payments within the date range
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByBuildingAndDateRange(
            Long buildingId, LocalDate startDate, LocalDate endDate) {
        log.debug("Retrieving payments for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        return paymentRepository.findByBuildingAndDateRange(buildingId, startDate, endDate);
    }
    
    /**
     * Calculates total payment amount for a building within a date range.
     * 
     * @param buildingId ID of the building
     * @param startDate Start date of the range
     * @param endDate End date of the range
     * @return Total payment amount
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalPaymentsByBuildingAndDateRange(
            Long buildingId, LocalDate startDate, LocalDate endDate) {
        log.debug("Calculating total payments for building ID: {} between {} and {}", 
                buildingId, startDate, endDate);
        BigDecimal total = paymentRepository.getTotalPaymentsByBuildingAndDateRange(
                buildingId, startDate, endDate);
        return total != null ? total : BigDecimal.ZERO;
    }
    
    /**
     * Calculates outstanding balance for a flat.
     * Sum of unpaid monthly dues minus any unallocated payments.
     * 
     * @param flatId ID of the flat
     * @return Outstanding balance amount
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateOutstandingBalance(Long flatId) {
        log.debug("Calculating outstanding balance for flat ID: {}", flatId);
        
        // Get total unpaid dues
        BigDecimal totalUnpaidDues = monthlyDueRepository
                .getTotalUnpaidDuesByFlat(flatId);
        
        // For simplicity in MVP, we assume all payments are allocated
        // In a more complex system, we'd track unallocated payment amounts
        
        return totalUnpaidDues != null ? totalUnpaidDues : BigDecimal.ZERO;
    }
    
    /**
     * Updates a payment (e.g., for corrections).
     * Handles optimistic locking via @Version field.
     * 
     * @param payment Payment entity with updates
     * @return Updated payment entity
     * @throws org.springframework.orm.ObjectOptimisticLockingFailureException if concurrent update detected
     */
    public Payment updatePayment(Payment payment) {
        log.info("Updating payment ID: {}", payment.getId());
        
        // Ensure payment exists
        Payment existingPayment = paymentRepository.findById(payment.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found with ID: " + payment.getId()));
        
        // Update allowed fields (amount changes would require reallocation logic)
        existingPayment.setPaymentMethod(payment.getPaymentMethod());
        existingPayment.setDescription(payment.getDescription());
        existingPayment.setReceiptNumber(payment.getReceiptNumber());
        
        // Note: If amount changes are allowed, we'd need to:
        // 1. Reverse the original allocation
        // 2. Re-allocate with new amount
        // This is complex and typically not allowed in financial systems
        
        return paymentRepository.save(existingPayment);
    }
    
    /**
     * Deletes a payment (soft delete recommended for audit trail).
     * In production, consider implementing soft delete instead.
     * 
     * @param paymentId ID of the payment to delete
     */
    public void deletePayment(Long paymentId) {
        log.warn("Deleting payment ID: {} - Consider implementing soft delete", paymentId);
        
        // In production, implement soft delete for audit trail
        // For MVP, we'll do hard delete but log the action
        
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Payment not found with ID: " + paymentId));
        
        // Reverse payment allocations before deletion
        reversePaymentAllocations(payment);
        
        paymentRepository.delete(payment);
        log.info("Payment ID: {} deleted successfully", paymentId);
    }
    
    /**
     * Reverses payment allocations from monthly dues.
     * Used when deleting or correcting payments.
     * 
     * @param payment Payment whose allocations to reverse
     */
    private void reversePaymentAllocations(Payment payment) {
        log.debug("Reversing allocations for payment ID: {}", payment.getId());
        
        // Find monthly dues that were paid on the same date as this payment
        // In a more complex system, we'd track payment-to-due mappings
        List<MonthlyDue> affectedDues = monthlyDueRepository
                .findByFlatIdAndPaymentDate(payment.getFlat().getId(), payment.getPaymentDate());
        
        for (MonthlyDue due : affectedDues) {
            // Reset to unpaid status
            // In production, we'd need more sophisticated tracking
            due.setStatus(MonthlyDue.DueStatus.UNPAID);
            due.setPaidAmount(BigDecimal.ZERO);
            due.setPaymentDate(null);
            
            monthlyDueRepository.save(due);
        }
    }
}