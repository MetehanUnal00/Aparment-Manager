package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.service.IAuditService;
import com.example.apartmentmanagerapi.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for monthly due-related events.
 * Handles asynchronous processing of monthly due events after transaction commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyDueEventListener {
    
    private final IAuditService auditService;
    
    /**
     * Handles monthly dues generated events.
     * Executes asynchronously after the transaction commits successfully.
     * 
     * @param event the monthly dues generated event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMonthlyDuesGenerated(MonthlyDuesGeneratedEvent event) {
        log.info("Processing MonthlyDuesGeneratedEvent for building {} - {} flats affected for {}/{}", 
                event.getBuildingId(), event.getNumberOfFlatsAffected(), 
                event.getMonth(), event.getYear());
        
        try {
            // Log the successful generation
            auditService.logSuccess(
                AuditLog.AuditAction.MONTHLY_DUE_GENERATED,
                "MonthlyDue",
                null,
                String.format("Generated monthly dues for %d flats in building %d for %02d/%d", 
                    event.getNumberOfFlatsAffected(), event.getBuildingId(), 
                    event.getMonth(), event.getYear())
            );
            
            // Future: Send monthly due notifications
            if (event.getNumberOfFlatsAffected() > 0) {
                log.info("Would send {} monthly due notifications for building {} for {}/{}", 
                        event.getNumberOfFlatsAffected(), event.getBuildingId(), 
                        event.getMonth(), event.getYear());
                // NotificationService.sendMonthlyDueNotifications(event.getBuildingId(), event.getYear(), event.getMonth());
            }
            
            // Future: Generate monthly financial report
            log.debug("Would generate monthly financial report for building {} for {}/{}", 
                    event.getBuildingId(), event.getMonth(), event.getYear());
            // ReportService.generateMonthlyReport(event.getBuildingId(), event.getYear(), event.getMonth());
            
        } catch (Exception e) {
            log.error("Error processing MonthlyDuesGeneratedEvent for building {}: {}", 
                    event.getBuildingId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handles monthly dues generation failures.
     * Logs the failure for audit purposes.
     * 
     * @param event the monthly dues generated event
     */
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleMonthlyDuesGenerationRollback(MonthlyDuesGeneratedEvent event) {
        log.warn("Monthly dues generation rolled back for building {} for {}/{}", 
                event.getBuildingId(), event.getMonth(), event.getYear());
        
        // Log the failure
        auditService.logFailure(
            AuditLog.AuditAction.MONTHLY_DUE_GENERATED,
            String.format("Failed to generate monthly dues for building %d for %02d/%d", 
                event.getBuildingId(), event.getMonth(), event.getYear()),
            "Transaction rolled back"
        );
    }
}