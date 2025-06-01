package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.AuditLog;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.service.IAuditService;
import com.example.apartmentmanagerapi.service.IContractDueGenerationService;
import com.example.apartmentmanagerapi.service.IContractNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Event listener for contract-related events
 * Handles due generation, notifications, and audit logging
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContractEventListener {
    
    private final IContractDueGenerationService dueGenerationService;
    private final IContractNotificationService notificationService;
    private final IAuditService auditService;
    
    /**
     * Handle contract creation - generate dues and send notifications
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractCreated(ContractCreatedEvent event) {
        log.info("Handling contract created event for contract ID: {}", event.getContract().getId());
        
        // Generate monthly dues if requested
        if (event.isGenerateDuesImmediately()) {
            try {
                List<MonthlyDue> generatedDues = dueGenerationService.generateDuesForContract(event.getContract());
                log.info("Generated {} monthly dues for contract ID: {}", 
                        generatedDues.size(), event.getContract().getId());
                
                // Audit the due generation
                auditService.logSuccess(AuditLog.AuditAction.CONTRACT_DUES_GENERATED, 
                    "Contract", event.getContract().getId(),
                    String.format("Generated %d monthly dues for contract", generatedDues.size()));
            } catch (Exception e) {
                log.error("Error generating dues for contract ID: {}", event.getContract().getId(), e);
                auditService.logFailure(AuditLog.AuditAction.CONTRACT_DUES_GENERATION_FAILED,
                    String.format("Failed to generate dues for contract %d", event.getContract().getId()),
                    e.getMessage());
            }
        }
        
        // Send notification asynchronously
        sendContractCreationNotificationAsync(event);
    }
    
    /**
     * Handle contract renewal - generate extension dues
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractRenewed(ContractRenewedEvent event) {
        log.info("Handling contract renewed event. Old contract ID: {}, New contract ID: {}", 
                event.getOldContract().getId(), event.getNewContract().getId());
        
        if (event.isGenerateDuesImmediately()) {
            try {
                // Generate dues starting from the old contract's end date + 1 day
                List<MonthlyDue> generatedDues = dueGenerationService.generateDuesForContractExtension(
                    event.getNewContract(), 
                    event.getOldContract().getEndDate().plusDays(1)
                );
                
                log.info("Generated {} extension dues for renewed contract ID: {}", 
                        generatedDues.size(), event.getNewContract().getId());
                
                auditService.logSuccess(AuditLog.AuditAction.CONTRACT_RENEWAL_DUES_GENERATED,
                    "Contract", event.getNewContract().getId(),
                    String.format("Generated %d dues for contract renewal", generatedDues.size()));
            } catch (Exception e) {
                log.error("Error generating dues for renewed contract ID: {}", 
                         event.getNewContract().getId(), e);
            }
        }
        
        // Send renewal notification
        sendContractRenewalNotificationAsync(event);
    }
    
    /**
     * Handle contract cancellation - cancel unpaid dues
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractCancelled(ContractCancelledEvent event) {
        log.info("Handling contract cancelled event for contract ID: {}", event.getContract().getId());
        
        if (event.isCancelUnpaidDues()) {
            try {
                int cancelledCount = dueGenerationService.cancelUnpaidDuesForContract(event.getContract());
                log.info("Cancelled {} unpaid dues for contract ID: {}", 
                        cancelledCount, event.getContract().getId());
                
                auditService.logSuccess(AuditLog.AuditAction.CONTRACT_DUES_CANCELLED,
                    "Contract", event.getContract().getId(),
                    String.format("Cancelled %d unpaid dues due to contract cancellation", cancelledCount));
            } catch (Exception e) {
                log.error("Error cancelling dues for contract ID: {}", event.getContract().getId(), e);
            }
        }
        
        // Send cancellation notification
        sendContractCancellationNotificationAsync(event);
    }
    
    /**
     * Handle contract modification - regenerate future dues
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContractModified(ContractModifiedEvent event) {
        log.info("Handling contract modified event. Old contract ID: {}, New contract ID: {}", 
                event.getOldContract().getId(), event.getNewContract().getId());
        
        try {
            // Regenerate dues from the effective date
            dueGenerationService.regenerateDuesForModifiedContract(
                event.getOldContract(), 
                event.getNewContract(), 
                event.getEffectiveDate()
            );
            
            auditService.logSuccess(AuditLog.AuditAction.CONTRACT_MODIFICATION_DUES_UPDATED,
                "Contract", event.getNewContract().getId(),
                String.format("Regenerated dues from %s due to contract modification", 
                            event.getEffectiveDate()));
        } catch (Exception e) {
            log.error("Error regenerating dues for modified contract ID: {}", 
                     event.getNewContract().getId(), e);
        }
        
        // Send modification notification
        sendContractModificationNotificationAsync(event);
    }
    
    /**
     * Handle contract status change
     */
    @EventListener
    public void handleContractStatusChanged(ContractStatusChangedEvent event) {
        log.info("Contract {} status changed from {} to {}", 
                event.getContract().getId(), event.getOldStatus(), event.getNewStatus());
        
        // Log the status change
        auditService.logSuccess(AuditLog.AuditAction.CONTRACT_STATUS_CHANGED,
            "Contract", event.getContract().getId(),
            String.format("Status changed from %s to %s %s", 
                        event.getOldStatus(), event.getNewStatus(),
                        event.isAutomaticChange() ? "(automatic)" : "(manual)"));
        
        // Send notifications for certain status changes
        if (event.getNewStatus() == Contract.ContractStatus.EXPIRED) {
            // Contract has expired, notify relevant parties
            notificationService.sendContractCancellationNotification(
                event.getContract(), 
                "Contract has expired"
            );
        }
    }
    
    // Async notification methods to avoid blocking the main transaction
    
    @Async
    protected void sendContractCreationNotificationAsync(ContractCreatedEvent event) {
        try {
            notificationService.sendContractCreationNotification(event.getContract());
        } catch (Exception e) {
            log.error("Error sending contract creation notification for contract ID: {}", 
                     event.getContract().getId(), e);
        }
    }
    
    @Async
    protected void sendContractRenewalNotificationAsync(ContractRenewedEvent event) {
        try {
            notificationService.sendContractRenewalNotification(
                event.getOldContract(), 
                event.getNewContract()
            );
        } catch (Exception e) {
            log.error("Error sending contract renewal notification for contract ID: {}", 
                     event.getNewContract().getId(), e);
        }
    }
    
    @Async
    protected void sendContractCancellationNotificationAsync(ContractCancelledEvent event) {
        try {
            notificationService.sendContractCancellationNotification(
                event.getContract(), 
                event.getCancellationReason()
            );
        } catch (Exception e) {
            log.error("Error sending contract cancellation notification for contract ID: {}", 
                     event.getContract().getId(), e);
        }
    }
    
    @Async
    protected void sendContractModificationNotificationAsync(ContractModifiedEvent event) {
        try {
            notificationService.sendContractModificationNotification(
                event.getOldContract(), 
                event.getNewContract()
            );
        } catch (Exception e) {
            log.error("Error sending contract modification notification for contract ID: {}", 
                     event.getNewContract().getId(), e);
        }
    }
}