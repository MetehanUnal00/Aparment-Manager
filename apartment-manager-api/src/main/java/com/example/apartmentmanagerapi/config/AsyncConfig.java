package com.example.apartmentmanagerapi.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

/**
 * Configuration for asynchronous processing.
 * Enables @Async annotation and configures thread pool for async operations.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig implements AsyncConfigurer {
    
    /**
     * Configure the thread pool executor for async operations
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - number of threads to keep in the pool
        executor.setCorePoolSize(2);
        
        // Maximum pool size
        executor.setMaxPoolSize(10);
        
        // Queue capacity - number of tasks to queue before creating new threads
        executor.setQueueCapacity(100);
        
        // Thread name prefix for easier debugging
        executor.setThreadNamePrefix("AuditAsync-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // Maximum time to wait for tasks to complete on shutdown
        executor.setAwaitTerminationSeconds(30);
        
        // Initialize the executor
        executor.initialize();
        
        log.info("Async executor configured with core pool size: {}, max pool size: {}", 
                executor.getCorePoolSize(), executor.getMaxPoolSize());
        
        return executor;
    }
    
    /**
     * Handle exceptions that occur in async methods
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("Exception in async method '{}' with parameters {}", 
                    method.getName(), objects, throwable);
        };
    }
}