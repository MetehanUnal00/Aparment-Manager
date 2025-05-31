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
 * Event listener for expense-related events.
 * Handles asynchronous processing of expense events after transaction commits.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ExpenseEventListener {
    
    private final IAuditService auditService;
    
    /**
     * Handles expense recorded events.
     * Executes asynchronously after the transaction commits successfully.
     * 
     * @param event the expense recorded event
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleExpenseRecorded(ExpenseRecordedEvent event) {
        log.info("Processing ExpenseRecordedEvent for expense {} of amount {} in category {} for building {}", 
                event.getExpenseId(), event.getAmount(), event.getCategory(), event.getBuildingId());
        
        try {
            // Log the successful expense creation
            auditService.logSuccess(
                AuditLog.AuditAction.EXPENSE_CREATED,
                "Expense",
                event.getExpenseId(),
                String.format("Expense of %s recorded in category %s for building %d%s", 
                    event.getAmount(), event.getCategory(), event.getBuildingId(),
                    event.isShouldDistributeToFlats() ? " (distributed to flats)" : "")
            );
            
            // Clear building financial caches to force refresh
            clearBuildingFinancialCaches(event.getBuildingId());
            
            // Future: Notify building manager about significant expenses
            if (event.getAmount().doubleValue() > 1000) {
                log.info("Would notify building manager about significant expense of {} for building {}", 
                        event.getAmount(), event.getBuildingId());
                // NotificationService.notifySignificantExpense(event.getBuildingId(), event.getAmount());
            }
            
        } catch (Exception e) {
            log.error("Error processing ExpenseRecordedEvent for expense {}: {}", 
                    event.getExpenseId(), e.getMessage(), e);
        }
    }
    
    /**
     * Clears the building financial caches when an expense is recorded.
     * This ensures that expense reports and statistics are always up-to-date.
     * 
     * @param buildingId the building ID whose caches should be cleared
     */
    @CacheEvict(value = {"buildingFinancials", "monthlyExpenseTotals"}, key = "#buildingId")
    public void clearBuildingFinancialCaches(Long buildingId) {
        log.debug("Cleared building financial caches for building {}", buildingId);
    }
    
    /**
     * Handles expense recording failures.
     * Logs the failure for audit purposes.
     * 
     * @param event the expense recorded event
     */
    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void handleExpenseRecordingRollback(ExpenseRecordedEvent event) {
        log.warn("Expense recording rolled back for expense {} of amount {} in category {} for building {}", 
                event.getExpenseId(), event.getAmount(), event.getCategory(), event.getBuildingId());
        
        // Log the failure
        auditService.logFailure(
            AuditLog.AuditAction.EXPENSE_CREATED,
            String.format("Failed to record expense of %s in category %s for building %d", 
                event.getAmount(), event.getCategory(), event.getBuildingId()),
            "Transaction rolled back"
        );
    }
}