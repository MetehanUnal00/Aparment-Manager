import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, timer, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { LoadingService } from '../../core/services/loading.service';
import { 
  MonthlyDueResponse, 
  MonthlyDueRequest,
  DebtorSummary,
  OverdueSummary
} from '../models/monthly-due.model';

/**
 * Service for managing monthly dues
 * Handles due generation, tracking, and debtor management
 */
@Injectable({
  providedIn: 'root'
})
export class MonthlyDueService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  private readonly loading = inject(LoadingService);
  
  // API endpoints
  private readonly baseUrl = '/monthly-dues';
  
  // Cache for debtor lists (5 minutes TTL due to frequent changes)
  private debtorCache = new Map<number, {
    data$: Observable<DebtorSummary[]>;
    time: number;
  }>();
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  /**
   * Get monthly dues for a specific flat
   * @param flatId The flat ID
   * @param year Optional year filter
   * @param month Optional month filter
   */
  getDuesByFlat(flatId: number, year?: number, month?: number): Observable<MonthlyDueResponse[]> {
    let url = `${this.baseUrl}/flat/${flatId}`;
    
    // Add query params if provided
    const params = new URLSearchParams();
    if (year) params.append('year', year.toString());
    if (month) params.append('month', month.toString());
    
    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    return this.api.get<MonthlyDueResponse[]>(url).pipe(
      tap(dues => console.log(`Fetched ${dues.length} monthly dues for flat ${flatId}`))
    );
  }

  /**
   * Get overdue payments for a building
   * @param buildingId The building ID
   * @param enablePolling Enable auto-refresh every 30 seconds
   */
  getOverdueDues(buildingId: number, enablePolling = false): Observable<OverdueSummary[]> {
    const url = `${this.baseUrl}/overdue/${buildingId}`;
    
    if (enablePolling) {
      // Poll every 30 seconds for dashboard
      return timer(0, 30000).pipe(
        switchMap(() => this.api.get<OverdueSummary[]>(url)),
        tap(overdue => console.log(`Found ${overdue.length} overdue payments`))
      );
    } else {
      return this.api.get<OverdueSummary[]>(url).pipe(
        tap(overdue => console.log(`Found ${overdue.length} overdue payments`))
      );
    }
  }

  /**
   * Get debtor report for a building
   * Uses caching to reduce API calls
   */
  getDebtorReport(buildingId: number, forceRefresh = false): Observable<DebtorSummary[]> {
    // Check cache validity
    const cached = this.debtorCache.get(buildingId);
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.CACHE_DURATION) {
      return cached.data$;
    }

    // Create new cached observable
    const debtors$ = this.api.get<DebtorSummary[]>(`${this.baseUrl}/debtors/${buildingId}`).pipe(
      tap(debtors => {
        console.log(`Debtor report for building ${buildingId}: ${debtors.length} debtors`);
        // Calculate total debt
        const totalDebt = debtors.reduce((sum, d) => sum + d.totalAmount, 0);
        console.log(`Total debt: ${this.formatCurrency(totalDebt)}`);
      }),
      shareReplay(1)
    );

    // Cache the result
    this.debtorCache.set(buildingId, {
      data$: debtors$,
      time: Date.now()
    });

    return debtors$;
  }

  /**
   * Generate monthly dues for a building
   * This can be a long operation, show loading indicator
   */
  generateMonthlyDues(request: MonthlyDueRequest): Observable<MonthlyDueResponse[]> {
    const buildingId = request.buildingId!;
    
    return this.loading.withLoading(
      this.api.post<MonthlyDueResponse[]>(`${this.baseUrl}/generate/${buildingId}`, request)
    ).pipe(
      tap(dues => {
        this.notification.success(`Generated ${dues.length} monthly dues successfully`);
        this.invalidateBuildingCache(buildingId);
      })
    );
  }

  /**
   * Create a single monthly due
   */
  createMonthlyDue(due: MonthlyDueRequest): Observable<MonthlyDueResponse> {
    return this.api.post<MonthlyDueResponse>(this.baseUrl, due).pipe(
      tap(created => {
        this.notification.success('Monthly due created successfully');
        if (due.buildingId) {
          this.invalidateBuildingCache(due.buildingId);
        }
      })
    );
  }

  /**
   * Mark a monthly due as paid
   */
  markAsPaid(dueId: number): Observable<MonthlyDueResponse> {
    return this.api.put<MonthlyDueResponse>(`${this.baseUrl}/${dueId}/paid`, {}).pipe(
      tap(updated => {
        this.notification.success('Monthly due marked as paid');
        // Clear all caches as we don't know the building ID
        this.debtorCache.clear();
      })
    );
  }

  /**
   * Update a monthly due
   */
  updateMonthlyDue(id: number, due: Partial<MonthlyDueRequest>): Observable<MonthlyDueResponse> {
    return this.api.put<MonthlyDueResponse>(`${this.baseUrl}/${id}`, due).pipe(
      tap(() => {
        this.notification.success('Monthly due updated successfully');
        this.debtorCache.clear(); // Clear all caches
      })
    );
  }

  /**
   * Delete a monthly due
   */
  deleteMonthlyDue(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.notification.success('Monthly due deleted successfully');
        this.debtorCache.clear(); // Clear all caches
      })
    );
  }

  /**
   * Send reminder emails to debtors
   */
  sendReminders(buildingId: number): Observable<{ sent: number; failed: number }> {
    return this.loading.withLoading(
      this.api.post<{ sent: number; failed: number }>(`${this.baseUrl}/reminders/${buildingId}`, {})
    ).pipe(
      tap(result => {
        this.notification.success(`Sent ${result.sent} reminder emails successfully`);
        if (result.failed > 0) {
          this.notification.warning(`Failed to send ${result.failed} emails`);
        }
      })
    );
  }

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
   * Invalidate cache for a specific building
   */
  private invalidateBuildingCache(buildingId: number): void {
    this.debtorCache.delete(buildingId);
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.debtorCache.clear();
  }
}