import { ErrorHandler, Injectable, Injector } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { NotificationService } from './notification.service';

/**
 * Global error handler service that catches and processes all uncaught errors.
 * Provides consistent error handling and logging throughout the application.
 */
@Injectable({
  providedIn: 'root'
})
export class GlobalErrorHandler implements ErrorHandler {
  constructor(private injector: Injector) {}

  /**
   * Handles all uncaught errors in the application
   * @param error The error object
   */
  handleError(error: Error | HttpErrorResponse): void {
    // Get notification service using injector to avoid circular dependency
    const notificationService = this.injector.get(NotificationService);

    // Log error to console for debugging
    console.error('Global Error Handler:', error);

    if (error instanceof HttpErrorResponse) {
      // Server error - already handled by HTTP interceptor
      this.logError(error);
    } else if (error instanceof TypeError) {
      // Type errors - usually programming errors
      notificationService.error(
        'Application Error',
        'An unexpected error occurred. Please refresh the page.'
      );
      this.logError(error);
    } else if (error instanceof Error) {
      // General errors
      const message = error.message || 'An unexpected error occurred';
      notificationService.error('Error', message);
      this.logError(error);
    } else {
      // Unknown errors
      notificationService.error(
        'Unknown Error',
        'An unexpected error occurred. Please try again.'
      );
      this.logError(error);
    }
  }

  /**
   * Logs error details for debugging and monitoring
   * @param error The error to log
   */
  private logError(error: any): void {
    const errorInfo = {
      timestamp: new Date().toISOString(),
      message: error.message || 'Unknown error',
      stack: error.stack || 'No stack trace available',
      url: window.location.href,
      userAgent: navigator.userAgent
    };

    // In production, this would send to a logging service
    console.error('Error Details:', errorInfo);
    
    // TODO: Implement remote error logging service integration
    // this.remoteLoggingService.logError(errorInfo);
  }
}