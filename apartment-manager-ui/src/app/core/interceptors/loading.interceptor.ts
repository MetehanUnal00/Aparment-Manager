import { Injectable } from '@angular/core';
import {
  HttpRequest,
  HttpHandler,
  HttpEvent,
  HttpInterceptor,
  HttpResponse
} from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, tap } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

/**
 * HTTP interceptor that automatically manages loading states for HTTP requests.
 * Shows loading indicators during API calls.
 */
@Injectable()
export class LoadingInterceptor implements HttpInterceptor {
  /**
   * Set of URLs that should not trigger loading indicators
   */
  private readonly excludedUrls: Set<string> = new Set([
    '/api/auth/refresh',
    '/api/health',
    '/api/config'
  ]);

  constructor(private loadingService: LoadingService) {}

  /**
   * Intercepts HTTP requests to manage loading states
   * @param request The outgoing request
   * @param next The next handler in the chain
   * @returns Observable of the HTTP event
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    // Check if this request should show loading indicator
    if (this.shouldShowLoading(request)) {
      // Generate a unique key for this request
      const loadingKey = this.generateLoadingKey(request);
      
      // Start loading
      this.loadingService.startLoading(loadingKey);

      return next.handle(request).pipe(
        tap(event => {
          // You can add custom logic here based on response
          if (event instanceof HttpResponse) {
            // Request completed successfully
          }
        }),
        finalize(() => {
          // Stop loading when request completes (success or error)
          this.loadingService.stopLoading(loadingKey);
        })
      );
    }

    // If loading should not be shown, just pass through
    return next.handle(request);
  }

  /**
   * Determines if loading indicator should be shown for this request
   * @param request The HTTP request
   * @returns true if loading should be shown
   */
  private shouldShowLoading(request: HttpRequest<unknown>): boolean {
    // Don't show loading for excluded URLs
    const url = request.url.toLowerCase();
    for (const excludedUrl of this.excludedUrls) {
      if (url.includes(excludedUrl)) {
        return false;
      }
    }

    // Check for custom header to disable loading
    if (request.headers.has('X-Skip-Loading')) {
      return false;
    }

    // Show loading for all other requests
    return true;
  }

  /**
   * Generates a unique loading key for the request
   * @param request The HTTP request
   * @returns Loading key string
   */
  private generateLoadingKey(request: HttpRequest<unknown>): string {
    // Extract meaningful part of URL for the key
    const urlParts = request.url.split('/').filter(part => part && part !== 'api');
    const endpoint = urlParts.slice(0, 2).join('-') || 'global';
    
    // Include method in key for better granularity
    return `${request.method.toLowerCase()}-${endpoint}`;
  }
}