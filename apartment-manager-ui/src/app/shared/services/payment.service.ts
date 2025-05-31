import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, throwError } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { 
  PaymentResponse, 
  PaymentRequest,
  PaymentSummary,
  PaymentAnalytics
} from '../models/payment.model';
import { DateRangeRequest } from '../models/common.model';

/**
 * Service for managing payments
 * Handles payment recording, updates, and analytics
 */
@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  
  // API endpoints
  private readonly baseUrl = '/payments';
  
  // Cache for payment summaries (10 minutes TTL)
  private summaryCache = new Map<string, {
    data$: Observable<PaymentSummary>;
    time: number;
  }>();
  private readonly CACHE_DURATION = 10 * 60 * 1000; // 10 minutes

  /**
   * Get payments for a specific flat
   * @param flatId The flat ID
   * @param dateRange Optional date range filter
   */
  getPaymentsByFlat(flatId: number, dateRange?: DateRangeRequest): Observable<PaymentResponse[]> {
    let url = `${this.baseUrl}/flat/${flatId}`;
    
    // Add date range query params if provided
    if (dateRange?.startDate || dateRange?.endDate) {
      const params = new URLSearchParams();
      if (dateRange.startDate) params.append('startDate', dateRange.startDate);
      if (dateRange.endDate) params.append('endDate', dateRange.endDate);
      url += `?${params.toString()}`;
    }

    return this.api.get<PaymentResponse[]>(url).pipe(
      tap(payments => console.log(`Fetched ${payments.length} payments for flat ${flatId}`))
    );
  }

  /**
   * Get all payments for a building
   * @param buildingId The building ID
   * @param dateRange Optional date range filter
   */
  getPaymentsByBuilding(buildingId: number, dateRange?: DateRangeRequest): Observable<PaymentResponse[]> {
    let url = `${this.baseUrl}/building/${buildingId}`;
    
    // Add date range query params if provided
    if (dateRange?.startDate || dateRange?.endDate) {
      const params = new URLSearchParams();
      if (dateRange.startDate) params.append('startDate', dateRange.startDate);
      if (dateRange.endDate) params.append('endDate', dateRange.endDate);
      url += `?${params.toString()}`;
    }

    return this.api.get<PaymentResponse[]>(url).pipe(
      tap(payments => console.log(`Fetched ${payments.length} payments for building ${buildingId}`))
    );
  }

  /**
   * Get payment summary for a building
   * Uses caching to reduce API calls
   */
  getPaymentSummary(buildingId: number): Observable<PaymentSummary> {
    const cacheKey = `summary-${buildingId}`;
    const cached = this.summaryCache.get(cacheKey);
    
    // Check cache validity
    if (cached && (Date.now() - cached.time) < this.CACHE_DURATION) {
      return cached.data$;
    }

    // Create new cached observable
    // Note: Using statistics endpoint as there's no separate summary endpoint
    const summary$ = this.api.get<PaymentSummary>(`${this.baseUrl}/building/${buildingId}/statistics`).pipe(
      tap(summary => console.log(`Payment summary for building ${buildingId}:`, summary)),
      shareReplay(1)
    );

    // Cache the result
    this.summaryCache.set(cacheKey, {
      data$: summary$,
      time: Date.now()
    });

    return summary$;
  }

  /**
   * Get payment analytics
   * Note: This endpoint doesn't exist in the backend yet
   * TODO: Implement when backend endpoint is available
   */
  // getPaymentAnalytics(buildingId: number, dateRange?: DateRangeRequest): Observable<PaymentAnalytics> {
  //   let url = `${this.baseUrl}/analytics/${buildingId}`;
  //   
  //   if (dateRange?.startDate || dateRange?.endDate) {
  //     const params = new URLSearchParams();
  //     if (dateRange.startDate) params.append('startDate', dateRange.startDate);
  //     if (dateRange.endDate) params.append('endDate', dateRange.endDate);
  //     url += `?${params.toString()}`;
  //   }
  //
  //   return this.api.get<PaymentAnalytics>(url);
  // }

  /**
   * Record a new payment
   * Implements optimistic locking via version field
   */
  createPayment(payment: PaymentRequest): Observable<PaymentResponse> {
    // Validate payment amount
    if (payment.amount <= 0) {
      this.notification.error('Payment amount must be greater than zero');
      return throwError(() => new Error('Invalid payment amount'));
    }

    return this.api.post<PaymentResponse>(this.baseUrl, payment).pipe(
      tap(created => {
        this.notification.success(`Payment of ${this.formatCurrency(created.amount)} recorded successfully`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Update an existing payment
   * Handles optimistic locking conflicts
   */
  updatePayment(id: number, payment: PaymentRequest): Observable<PaymentResponse> {
    return this.api.put<PaymentResponse>(`${this.baseUrl}/${id}`, payment).pipe(
      tap(updated => {
        this.notification.success(`Payment updated successfully`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Delete a payment
   */
  deletePayment(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.notification.success('Payment deleted successfully');
        this.invalidateCache();
      })
    );
  }

  /**
   * Bulk record payments for multiple flats
   * Note: This endpoint doesn't exist in the backend yet
   * TODO: Implement when backend endpoint is available
   */
  // bulkCreatePayments(payments: PaymentRequest[]): Observable<PaymentResponse[]> {
  //   return this.api.post<PaymentResponse[]>(`${this.baseUrl}/bulk`, payments).pipe(
  //     tap(created => {
  //       this.notification.success(`${created.length} payments recorded successfully`);
  //       this.invalidateCache();
  //     })
  //   );
  // }

  /**
   * Format currency for display
   */
  private formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  /**
   * Invalidate all caches
   */
  private invalidateCache(): void {
    this.summaryCache.clear();
  }

  /**
   * Clear cache for a specific building
   */
  invalidateBuildingCache(buildingId: number): void {
    const cacheKey = `summary-${buildingId}`;
    this.summaryCache.delete(cacheKey);
  }
}