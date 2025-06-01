package com.example.apartmentmanagerapi.scheduler;

import com.example.apartmentmanagerapi.service.IContractNotificationService;
import com.example.apartmentmanagerapi.service.IContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for contract management
 * Handles automatic status updates and notifications
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    value = "app.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ContractScheduledTasks {
    
    private final IContractService contractService;
    private final IContractNotificationService notificationService;
    
    /**
     * Update contract statuses daily at 1:00 AM
     * Changes PENDING to ACTIVE when start date is reached
     * Changes ACTIVE to EXPIRED when end date passes
     */
    @Scheduled(cron = "0 0 1 * * ?") // Every day at 1:00 AM
    public void updateContractStatuses() {
        log.info("Starting scheduled contract status update");
        try {
            contractService.updateContractStatuses();
            log.info("Contract status update completed successfully");
        } catch (Exception e) {
            log.error("Error updating contract statuses", e);
        }
    }
    
    /**
     * Send contract expiry notifications daily at 9:00 AM
     * Notifies about contracts expiring within 30 days
     */
    @Scheduled(cron = "0 0 9 * * ?") // Every day at 9:00 AM
    public void sendExpiryNotifications() {
        log.info("Starting scheduled contract expiry notifications");
        try {
            int daysAhead = 30; // Look for contracts expiring in next 30 days
            var notifications = notificationService.sendExpiryNotifications(daysAhead);
            log.info("Sent {} contract expiry notifications", notifications.size());
        } catch (Exception e) {
            log.error("Error sending contract expiry notifications", e);
        }
    }
    
    /**
     * Send urgent expiry notifications daily at 3:00 PM
     * Notifies about contracts expiring within 7 days
     */
    @Scheduled(cron = "0 0 15 * * ?") // Every day at 3:00 PM
    public void sendUrgentExpiryNotifications() {
        log.info("Starting scheduled urgent contract expiry notifications");
        try {
            int daysAhead = 7; // Look for contracts expiring in next 7 days
            var notifications = notificationService.sendExpiryNotifications(daysAhead);
            log.info("Sent {} urgent contract expiry notifications", notifications.size());
        } catch (Exception e) {
            log.error("Error sending urgent contract expiry notifications", e);
        }
    }
    
    /**
     * Check for renewable contracts weekly on Mondays at 10:00 AM
     * Identifies contracts that can be renewed and sends reminders
     */
    @Scheduled(cron = "0 0 10 ? * MON") // Every Monday at 10:00 AM
    public void checkRenewableContracts() {
        log.info("Starting scheduled renewable contracts check");
        try {
            var renewableContracts = contractService.getRenewableContracts(30);
            if (!renewableContracts.isEmpty()) {
                log.info("Found {} renewable contracts", renewableContracts.size());
                // Additional logic to send renewal reminders can be added here
            }
        } catch (Exception e) {
            log.error("Error checking renewable contracts", e);
        }
    }
    
    /**
     * Generate monthly contract report on the 1st of each month at 6:00 AM
     * Creates summary of contract statistics for the previous month
     */
    @Scheduled(cron = "0 0 6 1 * ?") // 1st of each month at 6:00 AM
    public void generateMonthlyContractReport() {
        log.info("Starting scheduled monthly contract report generation");
        try {
            // This is a placeholder for monthly reporting logic
            // In a real implementation, this would generate and send reports
            log.info("Monthly contract report generation completed");
        } catch (Exception e) {
            log.error("Error generating monthly contract report", e);
        }
    }
}