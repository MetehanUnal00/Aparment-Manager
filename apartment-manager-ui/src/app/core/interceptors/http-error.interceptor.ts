import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpErrorResponse
} from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Router } from '@angular/router';
import { AuthService } from '../../auth/services/auth.service';
import { NotificationService } from '../services/notification.service';

/**
 * HTTP interceptor that handles errors globally.
 * Provides consistent error handling and user feedback.
 */
@Injectable()
export class HttpErrorInterceptor implements HttpInterceptor {
  constructor(
    private router: Router,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  /**
   * Intercepts HTTP requests to handle errors
   * @param request The outgoing request
   * @param next The next handler in the chain
   * @returns Observable of the HTTP event
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Handle different error scenarios
        if (error.status === 401) {
          // Unauthorized - redirect to login
          this.handleUnauthorized();
        } else if (error.status === 403) {
          // Forbidden - show error message
          this.handleForbidden();
        } else if (error.status === 404) {
          // Not found
          this.handleNotFound(error);
        } else if (error.status === 409) {
          // Conflict
          this.handleConflict(error);
        } else if (error.status >= 500) {
          // Server error
          this.handleServerError(error);
        } else if (error.status === 0) {
          // Network error
          this.handleNetworkError();
        } else {
          // Other errors
          this.handleGenericError(error);
        }

        // Re-throw the error for specific handling in components if needed
        return throwError(() => error);
      })
    );
  }

  /**
   * Handles 401 Unauthorized errors
   */
  private handleUnauthorized(): void {
    this.authService.logout();
    this.notificationService.warning(
      'Session Expired',
      'Please log in again to continue.'
    );
    this.router.navigate(['/auth/login']);
  }

  /**
   * Handles 403 Forbidden errors
   */
  private handleForbidden(): void {
    this.notificationService.error(
      'Access Denied',
      'You do not have permission to perform this action.'
    );
  }

  /**
   * Handles 404 Not Found errors
   * @param error The HTTP error response
   */
  private handleNotFound(error: HttpErrorResponse): void {
    const message = this.getErrorMessage(error) || 'The requested resource was not found.';
    this.notificationService.warning('Not Found', message);
  }

  /**
   * Handles 409 Conflict errors
   * @param error The HTTP error response
   */
  private handleConflict(error: HttpErrorResponse): void {
    const message = this.getErrorMessage(error) || 'A conflict occurred with the current state.';
    this.notificationService.error('Conflict', message);
  }

  /**
   * Handles 5xx Server errors
   * @param error The HTTP error response
   */
  private handleServerError(error: HttpErrorResponse): void {
    const message = this.getErrorMessage(error) || 'An unexpected server error occurred.';
    this.notificationService.error(
      'Server Error',
      message + ' Please try again later.'
    );
  }

  /**
   * Handles network errors
   */
  private handleNetworkError(): void {
    this.notificationService.error(
      'Network Error',
      'Unable to connect to the server. Please check your internet connection.'
    );
  }

  /**
   * Handles generic errors
   * @param error The HTTP error response
   */
  private handleGenericError(error: HttpErrorResponse): void {
    const message = this.getErrorMessage(error) || 'An unexpected error occurred.';
    this.notificationService.error('Error', message);
  }

  /**
   * Extracts error message from HTTP error response
   * @param error The HTTP error response
   * @returns The error message
   */
  private getErrorMessage(error: HttpErrorResponse): string {
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
}