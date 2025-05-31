import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * Functional HTTP interceptor that handles errors globally.
 * Provides consistent error handling and user feedback.
 */
export const httpErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const authService = inject(AuthService);
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Handle different error scenarios
      if (error.status === 401) {
        // Unauthorized - handled by auth interceptor, but we can show a message
        if (!req.url.includes('/auth/login')) {
          notificationService.warning(
            'Session Expired',
            'Please log in again to continue.'
          );
        }
      } else if (error.status === 403) {
        // Forbidden
        notificationService.error(
          'Access Denied',
          'You do not have permission to perform this action.'
        );
      } else if (error.status === 404) {
        // Not found
        const message = getErrorMessage(error) || 'The requested resource was not found.';
        notificationService.warning('Not Found', message);
      } else if (error.status === 409) {
        // Conflict
        const message = getErrorMessage(error) || 'A conflict occurred with the current state.';
        notificationService.error('Conflict', message);
      } else if (error.status >= 500) {
        // Server error
        const message = getErrorMessage(error) || 'An unexpected server error occurred.';
        notificationService.error(
          'Server Error',
          message + ' Please try again later.'
        );
      } else if (error.status === 0) {
        // Network error
        notificationService.error(
          'Network Error',
          'Unable to connect to the server. Please check your internet connection.'
        );
      } else if (error.status !== 400) {
        // Other errors (excluding 400 which often has specific validation messages)
        const message = getErrorMessage(error) || 'An unexpected error occurred.';
        notificationService.error('Error', message);
      }

      // Re-throw the error for specific handling in components if needed
      return throwError(() => error);
    })
  );
};

/**
 * Extracts error message from HTTP error response
 * @param error The HTTP error response
 * @returns The error message
 */
function getErrorMessage(error: HttpErrorResponse): string {
  // Check for structured error response
  if (error.error && typeof error.error === 'object') {
    // Look for common error message properties
    return error.error.message || 
           error.error.error || 
           error.error.detail ||
           error.error.title ||
           JSON.stringify(error.error);
  }
  
  // Check for string error
  if (typeof error.error === 'string') {
    return error.error;
  }
  
  // Fallback to status text
  return error.statusText;
}