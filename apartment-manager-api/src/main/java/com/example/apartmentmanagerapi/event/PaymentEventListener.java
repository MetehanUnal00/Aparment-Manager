package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.service.IAuditService;
import com.example.apartmentmanagerapi.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for payment-related events.
 * Handles asynchronous processing of payment events after transaction commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventListener {
    
    private final IAuditService auditService;
    
    /**
     * Handles payment recorded events.
     * Executes asynchronously after the transaction commits successfully.
     * 
     * @param event the payment recorded event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentRecorded(PaymentRecordedEvent event) {
        log.info("Processing PaymentRecordedEvent for payment {} of amount {} for flat {}", 
                event.getPaymentId(), event.getAmount(), event.getFlatId());
        
        try {
            // Log the successful payment
            auditService.logSuccess(
                AuditLog.AuditAction.PAYMENT_CREATED,
                "Payment",
                event.getPaymentId(),
                String.format("Payment of %s recorded for flat %d", 
                    event.getAmount(), event.getFlatId())
            );
            
            // Future: Send payment confirmation email
            if (event.getTenantEmail() != null) {
                log.info("Would send payment confirmation to {} for amount {}", 
                        event.getTenantEmail(), event.getAmount());
                // EmailService.sendPaymentConfirmation(event.getTenantEmail(), event.getAmount());
            }
            
            // Clear building statistics cache to force refresh
            clearBuildingStatisticsCache(event.getBuildingId());
            
        } catch (Exception e) {
            log.error("Error processing PaymentRecordedEvent for payment {}: {}", 
                    event.getPaymentId(), e.getMessage(), e);
        }
    }
    
    /**
     * Clears the building statistics cache when a payment is recorded.
     * This ensures that financial statistics are always up-to-date.
     * 
     * @param buildingId the building ID whose cache should be cleared
     */
    @CacheEvict(value = "buildingStatistics", key = "#buildingId")
    public void clearBuildingStatisticsCache(Long buildingId) {
        log.debug("Cleared building statistics cache for building {}", buildingId);
    }
    
    /**
     * Handles payment recording failures.
     * Logs the failure for audit purposes.
     * 
     * @param event the payment recorded event
     */
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handlePaymentRecordingRollback(PaymentRecordedEvent event) {
        log.warn("Payment recording rolled back for payment {} of amount {} for flat {}", 
                event.getPaymentId(), event.getAmount(), event.getFlatId());
        
        // Log the failure
        auditService.logFailure(
            AuditLog.AuditAction.PAYMENT_CREATED,
            String.format("Failed to record payment of %s for flat %d", 
                event.getAmount(), event.getFlatId()),
            "Transaction rolled back"
        );
    }
}