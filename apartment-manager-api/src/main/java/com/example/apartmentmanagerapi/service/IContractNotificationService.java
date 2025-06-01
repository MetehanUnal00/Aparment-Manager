package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.dto.ContractExpiryNotification;
import com.example.apartmentmanagerapi.entity.Contract;

import java.util.List;

/**
 * Service interface for contract-related notifications
 */
public interface IContractNotificationService {
    
    /**
     * Send contract expiry notifications
     * @param daysAhead Number of days to look ahead for expiring contracts
     * @return List of sent notifications
     */
    List<ContractExpiryNotification> sendExpiryNotifications(int daysAhead);
    
    /**
     * Create expiry notification for a contract
     * @param contract The expiring contract
     * @return Expiry notification DTO
     */
    ContractExpiryNotification createExpiryNotification(Contract contract);
    
    /**
     * Send contract creation confirmation
     * @param contract The newly created contract
     */
    void sendContractCreationNotification(Contract contract);
    
    /**
     * Send contract renewal confirmation
     * @param oldContract The previous contract
     * @param newContract The renewed contract
     */
    void sendContractRenewalNotification(Contract oldContract, Contract newContract);
    
    /**
     * Send contract cancellation notification
     * @param contract The cancelled contract
     * @param reason Cancellation reason
     */
    void sendContractCancellationNotification(Contract contract, String reason);
    
    /**
     * Send contract modification notification
     * @param oldContract The superseded contract
     * @param newContract The new contract
     */
    void sendContractModificationNotification(Contract oldContract, Contract newContract);
    
    /**
     * Send overdue payment reminder for contract
     * @param contract Contract with overdue payments
     * @param overdueDuesCount Number of overdue dues
     */
    void sendOverduePaymentReminder(Contract contract, int overdueDuesCount);
    
    /**
     * Get notification recipients for a contract
     * @param contract The contract
     * @return List of email addresses
     */
    List<String> getNotificationRecipients(Contract contract);
    
    /**
     * Check if notifications are enabled for a building
     * @param buildingId Building ID
     * @return true if notifications are enabled
     */
    boolean areNotificationsEnabled(Long buildingId);
    
    /**
     * Schedule recurring expiry check
     * This method is called by the scheduled task
     */
    void scheduleExpiryCheck();
}