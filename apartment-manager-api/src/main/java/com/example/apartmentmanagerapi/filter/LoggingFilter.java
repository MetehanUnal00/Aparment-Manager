package com.example.apartmentmanagerapi.filter;

import com.example.apartmentmanagerapi.util.LoggingUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that adds logging context to all requests.
 * Sets correlation ID and user information in MDC for structured logging.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {
    
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Set correlation ID from header or generate new one
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = LoggingUtils.setCorrelationId();
            } else {
                LoggingUtils.setCorrelationId(correlationId);
            }
            
            // Add correlation ID to response header
            response.setHeader(CORRELATION_ID_HEADER, correlationId);
            
            // Set request information in MDC
            LoggingUtils.setRequestId(request.getRequestId());
            LoggingUtils.setOperation(request.getMethod() + " " + request.getRequestURI());
            
            // Set user context if authenticated
            setUserContext();
            
            // Log request start
            log.info("Request started: {} {} from IP: {}", 
                request.getMethod(), 
                request.getRequestURI(), 
                getClientIpAddress(request));
            
            // Continue with the filter chain
            filterChain.doFilter(request, response);
            
            // Log request completion
            long duration = System.currentTimeMillis() - startTime;
            log.info("Request completed: {} {} - Status: {} - Duration: {}ms", 
                request.getMethod(), 
                request.getRequestURI(), 
                response.getStatus(),
                duration);
            
        } finally {
            // Clear MDC context to prevent memory leaks
            LoggingUtils.clearContext();
        }
    }
    
    /**
     * Set user context in MDC from Spring Security
     */
    private void setUserContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                // Note: We don't have user ID in UserDetails, only username
                // You might need to enhance your UserDetails implementation to include user ID
                LoggingUtils.setUserContext(null, userDetails.getUsername());
            }
        }
    }
    
    /**
     * Get client IP address considering proxy headers
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
     * Don't filter actuator endpoints to reduce noise
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}