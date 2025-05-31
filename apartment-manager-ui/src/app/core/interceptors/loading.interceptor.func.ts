import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap, finalize } from 'rxjs/operators';
import { LoadingService } from '../services/loading.service';

/**
 * Set of URLs that should not trigger loading indicators
 */
const EXCLUDED_URLS: Set<string> = new Set([
  '/api/auth/refresh',
  '/api/health',
  '/api/config'
]);

/**
 * Functional HTTP interceptor that automatically manages loading states for HTTP requests.
 * Shows loading indicators during API calls.
 */
export const loadingInterceptor: HttpInterceptorFn = (req, next) => {
  const loadingService = inject(LoadingService);

  // Check if this request should show loading indicator
  if (shouldShowLoading(req)) {
    // Generate a unique key for this request
    const loadingKey = generateLoadingKey(req);
    
    // Start loading
    loadingService.startLoading(loadingKey);

    return next(req).pipe(
      tap(event => {
        // You can add custom logic here based on response
        if (event instanceof HttpResponse) {
          // Request completed successfully
        }
      }),
      finalize(() => {
        // Stop loading when request completes (success or error)
        loadingService.stopLoading(loadingKey);
      })
    );
  }

  // If loading should not be shown, just pass through
  return next(req);
};

/**
 * Determines if loading indicator should be shown for this request
 * @param request The HTTP request
 * @returns true if loading should be shown
 */
function shouldShowLoading(request: any): boolean {
  // Don't show loading for excluded URLs
  const url = request.url.toLowerCase();
  for (const excludedUrl of EXCLUDED_URLS) {
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
function generateLoadingKey(request: any): string {
  // Extract meaningful part of URL for the key
  const urlParts = request.url.split('/').filter((part: string) => part && part !== 'api');
  const endpoint = urlParts.slice(0, 2).join('-') || 'global';
  
  // Include method in key for better granularity
  return `${request.method.toLowerCase()}-${endpoint}`;
}