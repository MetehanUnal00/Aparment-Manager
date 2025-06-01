package com.example.apartmentmanagerapi.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity for storing audit logs of critical business operations.
 * This captures important actions like user authentication, payments, and monthly due generation.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_entity_type_id", columnList = "entity_type,entity_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    /**
     * Unique identifier for the audit log entry
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Timestamp when the action occurred
     */
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    /**
     * ID of the user who performed the action
     */
    @Column(name = "user_id")
    private Long userId;
    
    /**
     * Username of the user who performed the action
     */
    @Column(name = "username", nullable = false, length = 50)
    private String username;
    
    /**
     * The action that was performed
     */
    @Column(name = "action", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditAction action;
    
    /**
     * Type of entity affected by the action
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;
    
    /**
     * ID of the entity affected by the action
     */
    @Column(name = "entity_id")
    private Long entityId;
    
    /**
     * Description of the action with relevant details
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * IP address from which the action was performed
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    /**
     * User agent string of the client
     */
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    /**
     * Correlation ID for tracking related actions
     */
    @Column(name = "correlation_id", length = 36)
    private String correlationId;
    
    /**
     * Result of the action (SUCCESS or FAILURE)
     */
    @Column(name = "result", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AuditResult result;
    
    /**
     * Error message if the action failed
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    
    /**
     * Enum for audit actions
     */
    public enum AuditAction {
        // Authentication actions
        USER_LOGIN,
        USER_LOGOUT,
        LOGIN_FAILED,
        
        // Payment actions
        PAYMENT_CREATED,
        PAYMENT_UPDATED,
        PAYMENT_DELETED,
        
        // Monthly due actions
        MONTHLY_DUE_GENERATED,
        MONTHLY_DUE_MARKED_PAID,
        
        // User management actions
        USER_CREATED,
        USER_UPDATED,
        USER_ROLE_CHANGED,
        
        // Building/Flat management actions
        BUILDING_CREATED,
        BUILDING_UPDATED,
        FLAT_CREATED,
        FLAT_UPDATED,
        FLAT_DEACTIVATED,
        
        // Expense actions
        EXPENSE_CREATED,
        EXPENSE_UPDATED,
        EXPENSE_DELETED,
        
        // Contract actions
        CONTRACT_CREATED,
        CONTRACT_RENEWED,
        CONTRACT_CANCELLED,
        CONTRACT_MODIFIED,
        CONTRACT_STATUS_CHANGED,
        CONTRACT_DUES_GENERATED,
        CONTRACT_DUES_GENERATION_FAILED,
        CONTRACT_DUES_CANCELLED,
        CONTRACT_RENEWAL_DUES_GENERATED,
        CONTRACT_MODIFICATION_DUES_UPDATED
    }
    
    /**
     * Enum for audit results
     */
    public enum AuditResult {
        SUCCESS,
        FAILURE
    }
    
    /**
     * Helper method to create an audit log for a successful action
     */
    public static AuditLog success(Long userId, String username, AuditAction action, String description) {
        return AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .username(username)
                .action(action)
                .description(description)
                .result(AuditResult.SUCCESS)
                .build();
    }
    
    /**
     * Helper method to create an audit log for a failed action
     */
    public static AuditLog failure(Long userId, String username, AuditAction action, String description, String errorMessage) {
        return AuditLog.builder()
                .timestamp(LocalDateTime.now())
                .userId(userId)
                .username(username)
                .action(action)
                .description(description)
                .result(AuditResult.FAILURE)
                .errorMessage(errorMessage)
                .build();
    }
}