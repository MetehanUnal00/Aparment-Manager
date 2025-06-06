package com.example.apartmentmanagerapi.event;

import com.example.apartmentmanagerapi.entity.AuditLog;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.entity.MonthlyDue;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import com.example.apartmentmanagerapi.service.IAuditService;
import com.example.apartmentmanagerapi.service.IContractDueGenerationService;
import com.example.apartmentmanagerapi.service.IContractNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
    private final ContractRepository contractRepository;
    
    /**
     * Handle contract creation - generate dues and send notifications
     * Using @TransactionalEventListener to ensure contract is committed before processing
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleContractCreated(ContractCreatedEvent event) {
        log.info("=== CONTRACT EVENT LISTENER TRIGGERED ===");
        log.info("Handling contract created event for contract ID: {}, generateDues: {}", 
                event.getContract().getId(), event.isGenerateDuesImmediately());
        
        // Generate monthly dues if requested
        if (event.isGenerateDuesImmediately()) {
            log.info("Attempting to generate dues for contract ID: {}", event.getContract().getId());
            try {
                // Reload the contract to ensure it's attached to the current persistence context
                Contract contract = contractRepository.findById(event.getContract().getId())
                    .orElseThrow(() -> new RuntimeException("Contract not found: " + event.getContract().getId()));
                log.info("Reloaded contract ID: {}, duesGenerated: {}", contract.getId(), contract.isDuesGenerated());
                
                List<MonthlyDue> generatedDues = dueGenerationService.generateDuesForContract(contract);
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
        
        // Evict caches for the flat
        evictFlatCaches(event.getContract());
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
        
        // Evict caches for both old and new contracts
        evictFlatCaches(event.getOldContract());
        evictFlatCaches(event.getNewContract());
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
        
        // Evict caches for the flat
        evictFlatCaches(event.getContract());
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
        
        // Evict caches for both old and new contracts
        evictFlatCaches(event.getOldContract());
        evictFlatCaches(event.getNewContract());
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
    
    /**
     * Evict flat-related caches when contract changes
     * This ensures that flat lists show updated contract information
     */
    private void evictFlatCaches(Contract contract) {
        if (contract == null || contract.getFlat() == null) {
            return;
        }
        
        Long flatId = contract.getFlat().getId();
        Long buildingId = contract.getFlat().getApartmentBuilding().getId();
        
        // Evict the building's flat list cache
        evictBuildingFlatsCache(buildingId);
        
        // Evict individual flat caches
        evictFlatActiveContractCache(flatId);
        evictFlatOccupancySummaryCache(flatId);
        
        log.debug("Evicted caches for flat {} in building {} due to contract change", 
                 flatId, buildingId);
    }
    
    @CacheEvict(value = "flatsWithContracts", key = "#buildingId")
    public void evictBuildingFlatsCache(Long buildingId) {
        // Method implementation is handled by Spring's @CacheEvict
    }
    
    @CacheEvict(value = "flatActiveContract", key = "#flatId")
    public void evictFlatActiveContractCache(Long flatId) {
        // Method implementation is handled by Spring's @CacheEvict
    }
    
    @CacheEvict(value = "flatOccupancySummary", key = "#flatId")
    public void evictFlatOccupancySummaryCache(Long flatId) {
        // Method implementation is handled by Spring's @CacheEvict
    }
}