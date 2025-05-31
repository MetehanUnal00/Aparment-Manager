package com.example.apartmentmanagerapi.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class for structured logging with MDC support.
 * Provides methods to add contextual information to logs.
 */
@Component
public class LoggingUtils {
    
    /**
     * MDC key for correlation ID
     */
    public static final String CORRELATION_ID_KEY = "correlationId";
    
    /**
     * MDC key for user ID
     */
    public static final String USER_ID_KEY = "userId";
    
    /**
     * MDC key for username
     */
    public static final String USERNAME_KEY = "username";
    
    /**
     * MDC key for request ID
     */
    public static final String REQUEST_ID_KEY = "requestId";
    
    /**
     * MDC key for operation name
     */
    public static final String OPERATION_KEY = "operation";
    
    /**
     * Generate and set a new correlation ID in MDC
     * @return The generated correlation ID
     */
    public static String setCorrelationId() {
        String correlationId = generateCorrelationId();
        MDC.put(CORRELATION_ID_KEY, correlationId);
        return correlationId;
    }
    
    /**
     * Set correlation ID in MDC
     * @param correlationId The correlation ID to set
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_KEY, correlationId);
        }
    }
    
    /**
     * Get correlation ID from MDC
     * @return The correlation ID or null if not set
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_KEY);
    }
    
    /**
     * Set user information in MDC
     * @param userId The user ID
     * @param username The username
     */
    public static void setUserContext(Long userId, String username) {
        if (userId != null) {
            MDC.put(USER_ID_KEY, userId.toString());
        }
        if (username != null) {
            MDC.put(USERNAME_KEY, username);
        }
    }
    
    /**
     * Set operation name in MDC
     * @param operation The operation being performed
     */
    public static void setOperation(String operation) {
        if (operation != null) {
            MDC.put(OPERATION_KEY, operation);
        }
    }
    
    /**
     * Set request ID in MDC
     * @param requestId The request ID
     */
    public static void setRequestId(String requestId) {
        if (requestId != null) {
            MDC.put(REQUEST_ID_KEY, requestId);
        }
    }
    
    /**
     * Clear all MDC context
     */
    public static void clearContext() {
        MDC.clear();
    }
    
    /**
     * Clear specific MDC key
     * @param key The key to remove
     */
    public static void clearKey(String key) {
        MDC.remove(key);
    }
    
    /**
     * Generate a new correlation ID
     * @return A new UUID string
     */
    private static String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Execute a runnable with MDC context preserved
     * @param runnable The runnable to execute
     * @return A runnable that preserves MDC context
     */
    public static Runnable withContext(Runnable runnable) {
        // Capture current MDC context
        var contextMap = MDC.getCopyOfContextMap();
        
        return () -> {
            // Restore MDC context
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                runnable.run();
            } finally {
                // Clear MDC context
                MDC.clear();
            }
        };
    }
}