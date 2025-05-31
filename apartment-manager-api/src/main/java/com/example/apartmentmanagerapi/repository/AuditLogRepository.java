package com.example.apartmentmanagerapi.repository;

import com.example.apartmentmanagerapi.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for AuditLog entity.
 * Provides methods for querying audit logs.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    /**
     * Find audit logs by user ID with pagination
     * @param userId The user ID
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find audit logs by username with pagination
     * @param username The username
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByUsername(String username, Pageable pageable);
    
    /**
     * Find audit logs by action with pagination
     * @param action The audit action
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByAction(AuditLog.AuditAction action, Pageable pageable);
    
    /**
     * Find audit logs within a time range
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);
    
    /**
     * Find audit logs by entity type and ID
     * @param entityType The entity type
     * @param entityId The entity ID
     * @param pageable Pagination information
     * @return Page of audit logs
     */
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    
    /**
     * Find failed audit logs
     * @param result The result (should be FAILURE)
     * @param pageable Pagination information
     * @return Page of failed audit logs
     */
    Page<AuditLog> findByResult(AuditLog.AuditResult result, Pageable pageable);
    
    /**
     * Find audit logs by correlation ID
     * @param correlationId The correlation ID
     * @return List of related audit logs
     */
    List<AuditLog> findByCorrelationIdOrderByTimestamp(String correlationId);
    
    /**
     * Count failed login attempts for a user in a time window
     * @param username The username
     * @param action The action (LOGIN_FAILED)
     * @param since Time window start
     * @return Count of failed attempts
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.username = :username AND a.action = :action AND a.timestamp >= :since")
    long countFailedLoginAttempts(@Param("username") String username, 
                                  @Param("action") AuditLog.AuditAction action, 
                                  @Param("since") LocalDateTime since);
    
    /**
     * Find recent actions by a user
     * @param userId The user ID
     * @param limit Number of records to return
     * @return List of recent audit logs
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentActionsByUser(@Param("userId") Long userId, Pageable pageable);
    
    /**
     * Delete old audit logs
     * @param before Delete logs older than this timestamp
     * @return Number of deleted records
     */
    @Query("DELETE FROM AuditLog a WHERE a.timestamp < :before")
    int deleteOldAuditLogs(@Param("before") LocalDateTime before);
}