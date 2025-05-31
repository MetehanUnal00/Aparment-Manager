package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Service interface for audit logging functionality.
 * Provides asynchronous and synchronous logging of critical business actions.
 */
public interface IAuditService {

    /**
     * Logs a successful action asynchronously.
     * 
     * @param action the audit action type
     * @param description detailed description of the action
     */
    void logSuccess(AuditLog.AuditAction action, String description);

    /**
     * Logs a successful action with entity details asynchronously.
     * 
     * @param action the audit action type
     * @param entityType the type of entity affected
     * @param entityId the ID of the entity affected
     * @param description detailed description of the action
     */
    void logSuccess(AuditLog.AuditAction action, String entityType, Long entityId, String description);

    /**
     * Logs a failed action asynchronously.
     * 
     * @param action the audit action type
     * @param description detailed description of the action
     * @param errorMessage the error message associated with the failure
     */
    void logFailure(AuditLog.AuditAction action, String description, String errorMessage);

    /**
     * Logs an authentication action with user details.
     * Used for login/logout tracking and security monitoring.
     * 
     * @param userId the user ID (can be null for failed logins)
     * @param username the username attempting authentication
     * @param action the authentication action (LOGIN/LOGOUT)
     * @param description detailed description
     * @param result the result of the action (SUCCESS/FAILURE)
     * @param ipAddress the IP address of the request
     */
    void logAuthentication(Long userId, String username, AuditLog.AuditAction action, 
                          String description, AuditLog.AuditResult result, String ipAddress);

    /**
     * Logs an action synchronously.
     * Use only when immediate logging is critical.
     * 
     * @param action the audit action type
     * @param description detailed description of the action
     * @param result the result of the action
     * @param errorMessage error message if failed (can be null)
     */
    void logSync(AuditLog.AuditAction action, String description, 
                 AuditLog.AuditResult result, String errorMessage);

    /**
     * Retrieves audit logs for a specific user.
     * 
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of audit logs
     */
    Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable);

    /**
     * Retrieves audit logs by action type.
     * 
     * @param action the audit action type
     * @param pageable pagination information
     * @return page of audit logs
     */
    Page<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable);

    /**
     * Retrieves audit logs within a time range.
     * 
     * @param startTime the start time
     * @param endTime the end time
     * @param pageable pagination information
     * @return page of audit logs
     */
    Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * Counts recent failed login attempts for security monitoring.
     * 
     * @param username the username to check
     * @param minutes the time window in minutes
     * @return count of failed login attempts
     */
    long countRecentFailedLoginAttempts(String username, int minutes);

    /**
     * Cleans up old audit logs for maintenance.
     * 
     * @param daysToKeep number of days of logs to retain
     * @return number of deleted records
     */
    int cleanupOldAuditLogs(int daysToKeep);
}