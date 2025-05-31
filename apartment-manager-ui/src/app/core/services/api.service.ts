import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, retry, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

/**
 * Generic API service that provides HTTP operations with retry logic and error handling.
 * All API services should extend or use this service for consistency.
 */
@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiUrl = environment.apiUrl;
  private readonly defaultTimeout = 30000; // 30 seconds
  private readonly retryAttempts = 2;
  private readonly retryDelay = 1000; // 1 second

  constructor(private http: HttpClient) {}

  /**
   * Performs a GET request
   * @param endpoint The API endpoint
   * @param params Optional query parameters
   * @param headers Optional headers
   * @returns Observable of the response
   */
  get<T>(endpoint: string, params?: HttpParams, headers?: HttpHeaders): Observable<T> {
    const url = `${this.apiUrl}${endpoint}`;
    return this.http.get<T>(url, { params, headers }).pipe(
      timeout(this.defaultTimeout),
      retry({
        count: this.retryAttempts,
        delay: this.retryDelay,
        resetOnSuccess: true
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Performs a POST request
   * @param endpoint The API endpoint
   * @param body The request body
   * @param headers Optional headers
   * @returns Observable of the response
   */
  post<T>(endpoint: string, body: any, headers?: HttpHeaders): Observable<T> {
    const url = `${this.apiUrl}${endpoint}`;
    return this.http.post<T>(url, body, { headers }).pipe(
      timeout(this.defaultTimeout),
      retry({
        count: this.retryAttempts,
        delay: this.retryDelay,
        resetOnSuccess: true,
        excludedStatusCodes: [400, 401, 403, 404, 409] // Don't retry client errors
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Performs a PUT request
   * @param endpoint The API endpoint
   * @param body The request body
   * @param headers Optional headers
   * @returns Observable of the response
   */
  put<T>(endpoint: string, body: any, headers?: HttpHeaders): Observable<T> {
    const url = `${this.apiUrl}${endpoint}`;
    return this.http.put<T>(url, body, { headers }).pipe(
      timeout(this.defaultTimeout),
      retry({
        count: this.retryAttempts,
        delay: this.retryDelay,
        resetOnSuccess: true,
        excludedStatusCodes: [400, 401, 403, 404, 409]
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Performs a DELETE request
   * @param endpoint The API endpoint
   * @param headers Optional headers
   * @returns Observable of the response
   */
  delete<T>(endpoint: string, headers?: HttpHeaders): Observable<T> {
    const url = `${this.apiUrl}${endpoint}`;
    return this.http.delete<T>(url, { headers }).pipe(
      timeout(this.defaultTimeout),
      retry({
        count: this.retryAttempts,
        delay: this.retryDelay,
        resetOnSuccess: true,
        excludedStatusCodes: [400, 401, 403, 404, 409]
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Performs a PATCH request
   * @param endpoint The API endpoint
   * @param body The request body
   * @param headers Optional headers
   * @returns Observable of the response
   */
  patch<T>(endpoint: string, body: any, headers?: HttpHeaders): Observable<T> {
    const url = `${this.apiUrl}${endpoint}`;
    return this.http.patch<T>(url, body, { headers }).pipe(
      timeout(this.defaultTimeout),
      retry({
        count: this.retryAttempts,
        delay: this.retryDelay,
        resetOnSuccess: true,
        excludedStatusCodes: [400, 401, 403, 404, 409]
      }),
      catchError(this.handleError)
    );
  }

  /**
   * Creates HttpParams from an object
   * @param params Object containing query parameters
   * @returns HttpParams instance
   */
  createParams(params: { [key: string]: any }): HttpParams {
    let httpParams = new HttpParams();
    Object.keys(params).forEach(key => {
      const value = params[key];
      if (value !== null && value !== undefined) {
        if (Array.isArray(value)) {
          value.forEach(item => {
            httpParams = httpParams.append(key, item.toString());
          });
        } else {
          httpParams = httpParams.append(key, value.toString());
        }
      }
    });
    return httpParams;
  }

  /**
   * Handles HTTP errors
   * @param error The HTTP error response
   * @returns Observable that errors
   */
  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An unknown error occurred';
    
    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Client Error: ${error.error.message}`;
    } else if (error.status === 0) {
      // Network error
      errorMessage = 'Network error: Unable to connect to the server';
    } else {
      // Server-side error
      errorMessage = error.error?.message || error.message || `Server Error: ${error.status}`;
    }

    console.error('API Error:', {
      status: error.status,
      message: errorMessage,
      url: error.url,
      error: error.error
    });

    return throwError(() => ({
      status: error.status,
      message: errorMessage,
      details: error.error
    }));
  }
}