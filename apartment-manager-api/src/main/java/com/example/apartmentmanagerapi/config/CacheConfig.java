package com.example.apartmentmanagerapi.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for the application.
 * Uses Caffeine as the cache provider with different cache configurations
 * for various use cases.
 */
@Configuration
public class CacheConfig {
    
    /**
     * Configures the cache manager with Caffeine caches.
     * Different caches have different TTL and size configurations based on their use case.
     * 
     * @return CacheManager configured with Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Default cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(100));
        
        // Register specific caches with custom configurations
        registerCaches(cacheManager);
        
        return cacheManager;
    }
    
    /**
     * Registers specific caches with custom configurations.
     * 
     * @param cacheManager The cache manager to register caches with
     */
    private void registerCaches(CaffeineCacheManager cacheManager) {
        // Building statistics cache - refreshed every 5 minutes
        cacheManager.registerCustomCache("buildingStatistics",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        
        // Building financial summary cache - refreshed every 10 minutes
        cacheManager.registerCustomCache("buildingFinancials", 
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        
        // Monthly expense totals cache - refreshed every 15 minutes
        cacheManager.registerCustomCache("monthlyExpenseTotals",
                Caffeine.newBuilder()
                        .expireAfterWrite(15, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build());
        
        // Expense category breakdown cache - refreshed every 10 minutes
        cacheManager.registerCustomCache("expenseCategoryBreakdown",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build());
        
        // Debtor list cache - refreshed every 5 minutes (more critical data)
        cacheManager.registerCustomCache("debtorList",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(50)
                        .recordStats()
                        .build());
        
        // Payment summary cache - refreshed every 10 minutes
        cacheManager.registerCustomCache("paymentSummary",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(100)
                        .recordStats()
                        .build());
        
        // Flat balance cache - refreshed every 2 minutes (frequently accessed)
        cacheManager.registerCustomCache("flatBalance",
                Caffeine.newBuilder()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());
        
        // Flats with contracts cache - refreshed every 5 minutes
        // Used for flat lists with embedded contract information
        cacheManager.registerCustomCache("flatsWithContracts",
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1000)
                        .recordStats()
                        .build());
        
        // Individual flat active contract cache - refreshed every 10 minutes
        // Used for caching active contract info for individual flats
        cacheManager.registerCustomCache("flatActiveContract",
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(500)
                        .recordStats()
                        .build());
        
        // Flat occupancy summary cache - refreshed every 30 minutes
        // Historical data that changes less frequently
        cacheManager.registerCustomCache("flatOccupancySummary",
                Caffeine.newBuilder()
                        .expireAfterWrite(30, TimeUnit.MINUTES)
                        .maximumSize(200)
                        .recordStats()
                        .build());
    }
}