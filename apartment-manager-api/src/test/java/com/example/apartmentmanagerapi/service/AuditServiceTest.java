package com.example.apartmentmanagerapi.service;

import com.example.apartmentmanagerapi.entity.AuditLog;
import com.example.apartmentmanagerapi.repository.AuditLogRepository;
import com.example.apartmentmanagerapi.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService
 * Tests audit logging functionality including async and sync operations
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuditService auditService;

    private static final String TEST_CORRELATION_ID = "test-correlation-id";
    private static final String TEST_USERNAME = "testuser";
    private static final Long TEST_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // Set up security context
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Log success - Basic success logging")
    void logSuccess_BasicLogging() {
        // Arrange
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.USER_LOGIN, "User logged in successfully");
            
            // Assert - verify async execution by checking method was called
            // Note: In real async test, we'd need to wait or use CompletableFuture
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getAction() == AuditLog.AuditAction.USER_LOGIN &&
                log.getDescription().equals("User logged in successfully") &&
                log.getResult() == AuditLog.AuditResult.SUCCESS &&
                log.getCorrelationId().equals(TEST_CORRELATION_ID)
            ));
        }
    }

    @Test
    @DisplayName("Log success with entity - Entity information included")
    void logSuccess_WithEntity() {
        // Arrange
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.PAYMENT_CREATED, "Payment", 123L, "Payment recorded");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getEntityType().equals("Payment") &&
                log.getEntityId().equals(123L) &&
                log.getAction() == AuditLog.AuditAction.PAYMENT_CREATED
            ));
        }
    }

    @Test
    @DisplayName("Log failure - Failure logging with error message")
    void logFailure_WithErrorMessage() {
        // Arrange
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logFailure(AuditLog.AuditAction.LOGIN_FAILED, "Login attempt", "Invalid credentials");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getAction() == AuditLog.AuditAction.LOGIN_FAILED &&
                log.getResult() == AuditLog.AuditResult.FAILURE &&
                log.getErrorMessage().equals("Invalid credentials")
            ));
        }
    }

    @Test
    @DisplayName("Log sync - Synchronous logging")
    void logSync_SynchronousLogging() {
        // Arrange
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSync(
                AuditLog.AuditAction.MONTHLY_DUE_GENERATED, 
                "Generated monthly dues", 
                AuditLog.AuditResult.SUCCESS, 
                null
            );
            
            // Assert - should be called immediately (not async)
            verify(auditLogRepository).save(argThat(log -> 
                log.getAction() == AuditLog.AuditAction.MONTHLY_DUE_GENERATED &&
                log.getResult() == AuditLog.AuditResult.SUCCESS
            ));
        }
    }

    @Test
    @DisplayName("Log authentication - Authentication logging with IP address")
    void logAuthentication_WithIpAddress() {
        // Arrange
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        // Set up request context
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        when(request.getHeader("User-Agent")).thenReturn("Test Browser");
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logAuthentication(
                TEST_USER_ID,
                TEST_USERNAME,
                AuditLog.AuditAction.USER_LOGIN,
                "User logged in",
                AuditLog.AuditResult.SUCCESS,
                "192.168.1.1"
            );
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getUserId().equals(TEST_USER_ID) &&
                log.getUsername().equals(TEST_USERNAME) &&
                log.getIpAddress().equals("192.168.1.1") &&
                log.getUserAgent().equals("Test Browser")
            ));
        }
        
        // Clean up
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Log with security context - Extract user from security context")
    void logWithSecurityContext_ExtractUser() {
        // Arrange
        UserDetails userDetails = User.builder()
                .username(TEST_USERNAME)
                .password("password")
                .authorities("ROLE_USER")
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.FLAT_CREATED, "Flat created");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getUsername().equals(TEST_USERNAME)
            ));
        }
    }

    @Test
    @DisplayName("Log with request context - Extract IP address")
    void logWithRequestContext_ExtractIpAddress() {
        // Arrange
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test User Agent");
        
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.PAYMENT_CREATED, "Payment created");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getIpAddress().equals("192.168.1.1") && // Should use X-Forwarded-For
                log.getUserAgent().equals("Test User Agent")
            ));
        }
        
        // Clean up
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Get audit logs by user - Pagination")
    void getAuditLogsByUser_Pagination() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        AuditLog log2 = new AuditLog();
        Page<AuditLog> expectedPage = new PageImpl<>(Arrays.asList(log1, log2), pageable, 2);
        
        when(auditLogRepository.findByUserId(TEST_USER_ID, pageable)).thenReturn(expectedPage);
        
        // Act
        Page<AuditLog> result = auditService.getAuditLogsByUser(TEST_USER_ID, pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        
        verify(auditLogRepository).findByUserId(TEST_USER_ID, pageable);
    }

    @Test
    @DisplayName("Get audit logs by action - Filter by action")
    void getAuditLogsByAction_FilterByAction() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        AuditLog log1 = new AuditLog();
        Page<AuditLog> expectedPage = new PageImpl<>(Collections.singletonList(log1), pageable, 1);
        
        when(auditLogRepository.findByAction(AuditLog.AuditAction.USER_LOGIN, pageable))
                .thenReturn(expectedPage);
        
        // Act
        Page<AuditLog> result = auditService.getAuditLogsByAction(
                AuditLog.AuditAction.USER_LOGIN, pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(auditLogRepository).findByAction(AuditLog.AuditAction.USER_LOGIN, pageable);
    }

    @Test
    @DisplayName("Get audit logs by time range - Date filtering")
    void getAuditLogsByTimeRange_DateFiltering() {
        // Arrange
        LocalDateTime startTime = LocalDateTime.now().minusDays(7);
        LocalDateTime endTime = LocalDateTime.now();
        Pageable pageable = PageRequest.of(0, 10);
        
        AuditLog log1 = new AuditLog();
        Page<AuditLog> expectedPage = new PageImpl<>(Collections.singletonList(log1), pageable, 1);
        
        when(auditLogRepository.findByTimestampBetween(startTime, endTime, pageable))
                .thenReturn(expectedPage);
        
        // Act
        Page<AuditLog> result = auditService.getAuditLogsByTimeRange(startTime, endTime, pageable);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        
        verify(auditLogRepository).findByTimestampBetween(startTime, endTime, pageable);
    }

    @Test
    @DisplayName("Count recent failed login attempts")
    void countRecentFailedLoginAttempts() {
        // Arrange
        int minutes = 30;
        when(auditLogRepository.countFailedLoginAttempts(
                eq(TEST_USERNAME), 
                eq(AuditLog.AuditAction.LOGIN_FAILED), 
                any(LocalDateTime.class)
        )).thenReturn(5L);
        
        // Act
        long count = auditService.countRecentFailedLoginAttempts(TEST_USERNAME, minutes);
        
        // Assert
        assertThat(count).isEqualTo(5L);
        
        verify(auditLogRepository).countFailedLoginAttempts(
                eq(TEST_USERNAME), 
                eq(AuditLog.AuditAction.LOGIN_FAILED), 
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Cleanup old audit logs - Delete old records")
    void cleanupOldAuditLogs_DeleteOldRecords() {
        // Arrange
        int daysToKeep = 90;
        int expectedDeletedCount = 150;
        when(auditLogRepository.deleteOldAuditLogs(any(LocalDateTime.class)))
                .thenReturn(expectedDeletedCount);
        
        // Act
        int deletedCount = auditService.cleanupOldAuditLogs(daysToKeep);
        
        // Assert
        assertThat(deletedCount).isEqualTo(expectedDeletedCount);
        
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(auditLogRepository).deleteOldAuditLogs(dateCaptor.capture());
        
        // Verify the cutoff date is approximately 90 days ago
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime expectedDate = LocalDateTime.now().minusDays(daysToKeep);
        assertThat(capturedDate).isBetween(
                expectedDate.minusMinutes(1), 
                expectedDate.plusMinutes(1)
        );
    }

    @Test
    @DisplayName("Handle exception during async logging - Should not throw")
    void handleExceptionDuringAsyncLogging_ShouldNotThrow() {
        // Arrange
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act & Assert - should not throw exception
            auditService.logSuccess(AuditLog.AuditAction.PAYMENT_CREATED, "Payment created");
            
            // Verify save was attempted
            verify(auditLogRepository, timeout(1000)).save(any(AuditLog.class));
        }
    }

    @Test
    @DisplayName("Extract IP from X-Real-IP header")
    void extractIpFromXRealIpHeader() {
        // Arrange
        ServletRequestAttributes requestAttributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(requestAttributes);
        
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.1");
        
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.PAYMENT_CREATED, "Payment created");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getIpAddress().equals("203.0.113.1") // Should use X-Real-IP
            ));
        }
        
        // Clean up
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("Log with anonymous user - No user in security context")
    void logWithAnonymousUser_NoUserInSecurityContext() {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "anonymousUser", null, Collections.emptyList());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        
        AuditLog savedLog = new AuditLog();
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);
        
        try (MockedStatic<LoggingUtils> mockedUtils = mockStatic(LoggingUtils.class)) {
            mockedUtils.when(LoggingUtils::getCorrelationId).thenReturn(TEST_CORRELATION_ID);
            
            // Act
            auditService.logSuccess(AuditLog.AuditAction.EXPENSE_CREATED, "Expense created");
            
            // Assert
            verify(auditLogRepository, timeout(1000)).save(argThat(log -> 
                log.getUsername() == null && // Anonymous user should not have username
                log.getUserId() == null
            ));
        }
    }
}