import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, timer, switchMap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { LoadingService } from '../../core/services/loading.service';
import { 
  MonthlyDueResponse, 
  MonthlyDueRequest,
  DebtorInfo,
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
    data$: Observable<DebtorInfo[]>;
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
   * Get all monthly dues for a building
   * @param buildingId The building ID
   */
  getAllDuesForBuilding(buildingId: number): Observable<MonthlyDueResponse[]> {
    const url = `${this.baseUrl}/building/${buildingId}`;
    
    return this.api.get<MonthlyDueResponse[]>(url).pipe(
      tap(dues => console.log(`Fetched ${dues.length} monthly dues for building ${buildingId}`))
    );
  }

  /**
   * Get overdue payments for a building
   * @param buildingId The building ID
   * @param enablePolling Enable auto-refresh every 30 seconds
   */
  getOverdueDues(buildingId: number, enablePolling = false): Observable<OverdueSummary[]> {
    const url = `${this.baseUrl}/building/${buildingId}/overdue`;
    
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
  getDebtorReport(buildingId: number, forceRefresh = false): Observable<DebtorInfo[]> {
    // Check cache validity
    const cached = this.debtorCache.get(buildingId);
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.CACHE_DURATION) {
      return cached.data$;
    }

    // Create new cached observable
    const debtors$ = this.api.get<DebtorInfo[]>(`${this.baseUrl}/building/${buildingId}/debtors`).pipe(
      tap(debtors => {
        console.log(`Debtor report for building ${buildingId}: ${debtors.length} debtors`);
        // Calculate total debt
        const totalDebt = debtors.reduce((sum, d) => sum + d.totalDebt, 0);
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
   * Loading state is automatically handled by the loading interceptor
   */
  generateMonthlyDues(request: MonthlyDueRequest): Observable<MonthlyDueResponse[]> {
    const buildingId = request.buildingId!;
    
    return this.api.post<MonthlyDueResponse[]>(`${this.baseUrl}/generate`, request).pipe(
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
   * Update a monthly due
   * Note: The backend doesn't have a specific "mark as paid" endpoint,
   * so this updates the due with paid status
   */
  updateMonthlyDue(dueId: number, request: MonthlyDueRequest): Observable<MonthlyDueResponse> {
    return this.api.put<MonthlyDueResponse>(`${this.baseUrl}/${dueId}`, request).pipe(
      tap(updated => {
        this.notification.success('Monthly due updated successfully');
        // Clear all caches as we don't know the building ID
        this.debtorCache.clear();
      })
    );
  }


  /**
   * Cancel a monthly due
   * Uses the DELETE endpoint with /cancel suffix
   */
  cancelMonthlyDue(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}/cancel`).pipe(
      tap(() => {
        this.notification.success('Monthly due cancelled successfully');
        this.debtorCache.clear(); // Clear all caches
      })
    );
  }

  /**
   * Send reminder emails to debtors
   * Note: This endpoint doesn't exist in the backend yet
   * TODO: Implement when backend endpoint is available
   */
  // sendReminders(buildingId: number): Observable<{ sent: number; failed: number }> {
  //   return this.api.post<{ sent: number; failed: number }>(`${this.baseUrl}/reminders/${buildingId}`, {}).pipe(
  //     tap(result => {
  //       this.notification.success(`Sent ${result.sent} reminder emails successfully`);
  //       if (result.failed > 0) {
  //         this.notification.warning(`Failed to send ${result.failed} emails`);
  //       }
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