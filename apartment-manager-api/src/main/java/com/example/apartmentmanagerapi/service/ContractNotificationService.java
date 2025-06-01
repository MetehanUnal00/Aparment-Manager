package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.ContractExpiryNotification;
import com.example.apartmentmanagerapi.entity.Contract;
import com.example.apartmentmanagerapi.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of contract notification service
 * Currently provides stub implementation - actual email/SMS sending would be added later
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ContractNotificationService implements IContractNotificationService {
    
    private final ContractRepository contractRepository;
    
    /**
     * Send contract expiry notifications for contracts expiring within specified days
     * @param daysAhead Number of days to look ahead for expiring contracts
     * @return List of sent notifications
     */
    @Override
    public List<ContractExpiryNotification> sendExpiryNotifications(int daysAhead) {
        log.debug("Checking for contracts expiring within {} days", daysAhead);
        
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);
        
        // Find contracts expiring in the specified period
        List<Contract> expiringContracts = contractRepository.findExpiringContracts(startDate, endDate);
        
        List<ContractExpiryNotification> notifications = new ArrayList<>();
        for (Contract contract : expiringContracts) {
            ContractExpiryNotification notification = createExpiryNotification(contract);
            notifications.add(notification);
            
            // In a real implementation, this would send actual emails/SMS
            log.info("Contract expiry notification prepared for contract ID: {}, flat: {}, expires on: {}", 
                contract.getId(), 
                contract.getFlat().getFlatNumber(), 
                contract.getEndDate());
        }
        
        return notifications;
    }
    
    /**
     * Create expiry notification for a contract
     * @param contract The expiring contract
     * @return Expiry notification DTO
     */
    @Override
    public ContractExpiryNotification createExpiryNotification(Contract contract) {
        return ContractExpiryNotification.builder()
            .contractId(contract.getId())
            .flatNumber(contract.getFlat().getFlatNumber())
            .buildingName(contract.getFlat().getApartmentBuilding().getName())
            .endDate(contract.getEndDate())
            .daysUntilExpiry((int) LocalDate.now().until(contract.getEndDate()).getDays())
            .tenantName(contract.getTenantName())
            .tenantContact(contract.getTenantContact())
            .build();
    }
    
    /**
     * Send contract creation confirmation
     * @param contract The newly created contract
     */
    @Override
    public void sendContractCreationNotification(Contract contract) {
        log.info("Contract creation notification for contract ID: {}", contract.getId());
        // Stub implementation - actual notification sending would be added here
    }
    
    /**
     * Send contract renewal confirmation
     * @param oldContract The previous contract
     * @param newContract The renewed contract
     */
    @Override
    public void sendContractRenewalNotification(Contract oldContract, Contract newContract) {
        log.info("Contract renewal notification - old contract ID: {}, new contract ID: {}", 
            oldContract.getId(), newContract.getId());
        // Stub implementation - actual notification sending would be added here
    }
    
    /**
     * Send contract cancellation notification
     * @param contract The cancelled contract
     * @param reason Cancellation reason
     */
    @Override
    public void sendContractCancellationNotification(Contract contract, String reason) {
        log.info("Contract cancellation notification for contract ID: {}, reason: {}", 
            contract.getId(), reason);
        // Stub implementation - actual notification sending would be added here
    }
    
    /**
     * Send contract modification notification
     * @param oldContract The superseded contract
     * @param newContract The new contract
     */
    @Override
    public void sendContractModificationNotification(Contract oldContract, Contract newContract) {
        log.info("Contract modification notification - old contract ID: {}, new contract ID: {}", 
            oldContract.getId(), newContract.getId());
        // Stub implementation - actual notification sending would be added here
    }
    
    /**
     * Send overdue payment reminder for contract
     * @param contract Contract with overdue payments
     * @param overdueDuesCount Number of overdue dues
     */
    @Override
    public void sendOverduePaymentReminder(Contract contract, int overdueDuesCount) {
        log.info("Overdue payment reminder for contract ID: {}, overdue count: {}", 
            contract.getId(), overdueDuesCount);
        // Stub implementation - actual notification sending would be added here
    }
    
    /**
     * Get notification recipients for a contract
     * Currently returns tenant email/phone if available
     * @param contract The contract
     * @return List of email addresses
     */
    @Override
    public List<String> getNotificationRecipients(Contract contract) {
        List<String> recipients = new ArrayList<>();
        
        // Add tenant contact if available
        if (contract.getTenantEmail() != null && !contract.getTenantEmail().isEmpty()) {
            recipients.add(contract.getTenantEmail());
        }
        
        // Add building managers' emails
        contract.getFlat().getApartmentBuilding().getUserAssignments().stream()
            .filter(assignment -> assignment.getUser() != null)
            .map(assignment -> assignment.getUser().getEmail())
            .forEach(recipients::add);
        
        return recipients;
    }
    
    /**
     * Check if notifications are enabled for a building
     * Currently always returns true - configuration could be added later
     * @param buildingId Building ID
     * @return true if notifications are enabled
     */
    @Override
    public boolean areNotificationsEnabled(Long buildingId) {
        // In a real implementation, this would check building or system settings
        return true;
    }
    
    /**
     * Schedule recurring expiry check
     * This method is called by the scheduled task
     */
    @Override
    public void scheduleExpiryCheck() {
        log.debug("Running scheduled expiry check");
        // This method is called by the scheduler
        sendExpiryNotifications(30);
    }
}