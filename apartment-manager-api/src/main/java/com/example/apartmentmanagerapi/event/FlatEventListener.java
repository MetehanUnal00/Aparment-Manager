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
 * Event listener for flat-related events.
 * Handles asynchronous processing of flat events after transaction commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlatEventListener {
    
    private final IAuditService auditService;
    
    /**
     * Handles flat creation events.
     * Executes asynchronously after the transaction commits successfully.
     * 
     * @param event the flat created event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFlatCreated(FlatCreatedEvent event) {
        log.info("Processing FlatCreatedEvent for flat {} in building {}", 
                event.getFlatNumber(), event.getBuildingId());
        
        try {
            // Log the successful flat creation
            auditService.logSuccess(
                AuditLog.AuditAction.FLAT_CREATED,
                "Flat",
                event.getFlatId(),
                String.format("Flat %s created in building %d", 
                    event.getFlatNumber(), event.getBuildingId())
            );
            
            // Future: Send welcome email to tenant
            if (event.getTenantEmail() != null) {
                log.info("Would send welcome email to {} for flat {}", 
                        event.getTenantEmail(), event.getFlatNumber());
                // EmailService.sendWelcomeEmail(event.getTenantEmail(), event.getTenantName());
            }
            
            // Future: Update building statistics cache
            log.debug("Would update building statistics for building {}", event.getBuildingId());
            // BuildingStatisticsService.updateStatistics(event.getBuildingId());
            
        } catch (Exception e) {
            log.error("Error processing FlatCreatedEvent for flat {}: {}", 
                    event.getFlatId(), e.getMessage(), e);
        }
    }
    
    /**
     * Handles flat creation failures.
     * Logs the failure for audit purposes.
     * 
     * @param event the flat created event
     */
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleFlatCreationRollback(FlatCreatedEvent event) {
        log.warn("Flat creation rolled back for flat {} in building {}", 
                event.getFlatNumber(), event.getBuildingId());
        
        // Log the failure
        auditService.logFailure(
            AuditLog.AuditAction.FLAT_CREATED,
            String.format("Failed to create flat %s in building %d", 
                event.getFlatNumber(), event.getBuildingId()),
            "Transaction rolled back"
        );
    }
}