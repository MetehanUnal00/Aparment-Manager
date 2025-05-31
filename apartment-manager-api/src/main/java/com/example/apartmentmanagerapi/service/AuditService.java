package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.AuditLog;
import com.example.apartmentmanagerapi.entity.User;
import com.example.apartmentmanagerapi.repository.AuditLogRepository;
import com.example.apartmentmanagerapi.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing audit logs.
 * Handles logging of critical business operations asynchronously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService implements IAuditService {
    
    private final AuditLogRepository auditLogRepository;
    
    /**
     * Log a successful action asynchronously
     * @param action The action performed
     * @param description Description of the action
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(AuditLog.AuditAction action, String description) {
        try {
            AuditLog auditLog = createAuditLog(action, description, AuditLog.AuditResult.SUCCESS, null);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action: {} - {}", action, description);
        } catch (Exception e) {
            log.error("Failed to create audit log for action: {}", action, e);
        }
    }
    
    /**
     * Log a successful action with entity information asynchronously
     * @param action The action performed
     * @param entityType The type of entity affected
     * @param entityId The ID of the entity affected
     * @param description Description of the action
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSuccess(AuditLog.AuditAction action, String entityType, Long entityId, String description) {
        try {
            AuditLog auditLog = createAuditLog(action, description, AuditLog.AuditResult.SUCCESS, null);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for action: {} on {} {}", action, entityType, entityId);
        } catch (Exception e) {
            log.error("Failed to create audit log for action: {}", action, e);
        }
    }
    
    /**
     * Log a failed action asynchronously
     * @param action The action attempted
     * @param description Description of the action
     * @param errorMessage The error that occurred
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFailure(AuditLog.AuditAction action, String description, String errorMessage) {
        try {
            AuditLog auditLog = createAuditLog(action, description, AuditLog.AuditResult.FAILURE, errorMessage);
            auditLogRepository.save(auditLog);
            log.debug("Audit log created for failed action: {} - {}", action, errorMessage);
        } catch (Exception e) {
            log.error("Failed to create audit log for failed action: {}", action, e);
        }
    }
    
    /**
     * Log a synchronous action (for critical operations that must be logged immediately)
     * @param action The action performed
     * @param description Description of the action
     * @param result The result of the action
     * @param errorMessage Error message if failed
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSync(AuditLog.AuditAction action, String description, AuditLog.AuditResult result, String errorMessage) {
        try {
            AuditLog auditLog = createAuditLog(action, description, result, errorMessage);
            auditLogRepository.save(auditLog);
            log.debug("Synchronous audit log created for action: {} - Result: {}", action, result);
        } catch (Exception e) {
            log.error("Failed to create synchronous audit log for action: {}", action, e);
        }
    }
    
    /**
     * Log authentication action with specific user information
     * @param userId User ID
     * @param username Username
     * @param action The authentication action
     * @param description Description
     * @param result Success or failure
     * @param ipAddress IP address
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthentication(Long userId, String username, AuditLog.AuditAction action, 
                                  String description, AuditLog.AuditResult result, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .description(description)
                    .result(result)
                    .ipAddress(ipAddress)
                    .correlationId(LoggingUtils.getCorrelationId())
                    .build();
            
            // Add request details if available
            getRequestDetails().ifPresent(request -> {
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            });
            
            auditLogRepository.save(auditLog);
            log.debug("Authentication audit log created for user: {} - Action: {}", username, action);
        } catch (Exception e) {
            log.error("Failed to create authentication audit log", e);
        }
    }
    
    /**
     * Get audit logs by user with pagination
     * @param userId User ID
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }
    
    /**
     * Get audit logs by action with pagination
     * @param action The action
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByAction(AuditLog.AuditAction action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
    
    /**
     * Get audit logs within a time range
     * @param startTime Start time
     * @param endTime End time
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(startTime, endTime, pageable);
    }
    
    /**
     * Count failed login attempts for a user in the last specified minutes
     * @param username Username
     * @param minutes Time window in minutes
     * @return Number of failed attempts
     */
    @Transactional(readOnly = true)
    public long countRecentFailedLoginAttempts(String username, int minutes) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutes);
        return auditLogRepository.countFailedLoginAttempts(username, AuditLog.AuditAction.LOGIN_FAILED, since);
    }
    
    /**
     * Clean up old audit logs
     * @param daysToKeep Number of days to keep audit logs
     * @return Number of deleted records
     */
    @Transactional
    public int cleanupOldAuditLogs(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        int deletedCount = auditLogRepository.deleteOldAuditLogs(cutoffDate);
        log.info("Cleaned up {} audit logs older than {} days", deletedCount, daysToKeep);
        return deletedCount;
    }
    
    /**
     * Create an audit log with current context information
     */
    private AuditLog createAuditLog(AuditLog.AuditAction action, String description, 
                                   AuditLog.AuditResult result, String errorMessage) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTimestamp(LocalDateTime.now());
        auditLog.setAction(action);
        auditLog.setDescription(description);
        auditLog.setResult(result);
        auditLog.setErrorMessage(errorMessage);
        auditLog.setCorrelationId(LoggingUtils.getCorrelationId());
        
        // Get user information from security context
        getCurrentUser().ifPresent(userInfo -> {
            auditLog.setUserId(userInfo.userId);
            auditLog.setUsername(userInfo.username);
        });
        
        // Get request information
        getRequestDetails().ifPresent(request -> {
            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        });
        
        return auditLog;
    }
    
    /**
     * Get current user information from security context
     */
    private Optional<UserInfo> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = null;
            Long userId = null;
            
            if (authentication.getPrincipal() instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) authentication.getPrincipal();
                username = userDetails.getUsername();
                // Note: You might need to cast to your custom UserDetails implementation
                // to get the user ID
            } else if (authentication.getPrincipal() instanceof String) {
                username = (String) authentication.getPrincipal();
            }
            
            if (username != null && !"anonymousUser".equals(username)) {
                return Optional.of(new UserInfo(userId, username));
            }
        }
        return Optional.empty();
    }
    
    /**
     * Get current HTTP request
     */
    private Optional<HttpServletRequest> getRequestDetails() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return Optional.ofNullable(attributes.getRequest());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Helper class to hold user information
     */
    private static class UserInfo {
        final Long userId;
        final String username;
        
        UserInfo(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }
    }
}