import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, timer, switchMap, BehaviorSubject, map } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { 
  FlatResponse, 
  FlatRequest,
  FlatFinancialSummary,
  FlatBalance
} from '../models/flat.model';

/**
 * Service for managing flats
 * Handles all flat-related API operations with caching and polling support
 */
@Injectable({
  providedIn: 'root'
})
export class FlatService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  
  // API endpoints
  private readonly baseUrl = '/apartment-buildings';
  
  // Cache for flats by building (key: buildingId)
  private flatsCache = new Map<number, {
    data$: Observable<FlatResponse[]>;
    time: number;
  }>();
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  // Polling subjects for auto-refresh
  private pollingSubjects = new Map<number, BehaviorSubject<boolean>>();

  /**
   * Get all flats for a building
   * Supports caching and optional polling
   */
  getFlatsByBuilding(
    buildingId: number, 
    options: { forceRefresh?: boolean; enablePolling?: boolean } = {}
  ): Observable<FlatResponse[]> {
    const { forceRefresh = false, enablePolling = false } = options;

    // Check cache validity
    const cached = this.flatsCache.get(buildingId);
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.CACHE_DURATION) {
      return cached.data$;
    }

    // Create new observable
    const flats$ = this.createFlatsObservable(buildingId, enablePolling);
    
    // Cache the observable
    this.flatsCache.set(buildingId, {
      data$: flats$,
      time: Date.now()
    });

    return flats$;
  }

  /**
   * Create observable for flats with optional polling
   */
  private createFlatsObservable(buildingId: number, enablePolling: boolean): Observable<FlatResponse[]> {
    if (enablePolling) {
      // Get or create polling subject
      let pollingSubject = this.pollingSubjects.get(buildingId);
      if (!pollingSubject) {
        pollingSubject = new BehaviorSubject<boolean>(true);
        this.pollingSubjects.set(buildingId, pollingSubject);
      }

      // Poll every 30 seconds when active
      return timer(0, 30000).pipe(
        switchMap(() => pollingSubject.asObservable()),
        switchMap(isActive => 
          isActive 
            ? this.api.get<FlatResponse[]>(`${this.baseUrl}/${buildingId}/flats`)
            : []
        ),
        shareReplay(1)
      );
    } else {
      // Single fetch with caching
      return this.api.get<FlatResponse[]>(`${this.baseUrl}/${buildingId}/flats`).pipe(
        tap(() => console.log(`Fetched flats for building ${buildingId}`)),
        shareReplay(1)
      );
    }
  }

  /**
   * Stop polling for a specific building
   */
  stopPolling(buildingId: number): void {
    const subject = this.pollingSubjects.get(buildingId);
    if (subject) {
      subject.next(false);
      subject.complete();
      this.pollingSubjects.delete(buildingId);
    }
  }

  /**
   * Get a single flat by ID
   * Note: This requires both buildingId and flatId in the actual API
   */
  getFlat(buildingId: number, flatId: number): Observable<FlatResponse> {
    return this.api.get<FlatResponse>(`${this.baseUrl}/${buildingId}/flats/${flatId}`).pipe(
      tap(flat => console.log(`Fetched flat ${flatId}:`, flat))
    );
  }

  /**
   * Get flat balance
   * Balance is frequently accessed, cache for 2 minutes
   * Note: The backend doesn't have a separate balance endpoint, it's part of flat response
   */
  getFlatBalance(buildingId: number, flatId: number): Observable<FlatBalance> {
    // Since there's no separate balance endpoint, get the flat data
    return this.getFlat(buildingId, flatId).pipe(
      map(flat => ({
        flatId: flat.id,
        currentBalance: flat.currentBalance || 0,
        totalDues: 0, // Not provided in flat response
        totalPayments: 0 // Not provided in flat response
      } as FlatBalance)),
      tap(balance => console.log(`Flat ${flatId} balance:`, balance.currentBalance))
    );
  }

  /**
   * Get flat financial summary
   */
  getFlatFinancialSummary(buildingId: number, flatId: number): Observable<FlatFinancialSummary> {
    return this.api.get<FlatFinancialSummary>(`${this.baseUrl}/${buildingId}/flats/${flatId}/financial-info`);
  }

  /**
   * Create a new flat
   */
  createFlat(flat: FlatRequest): Observable<FlatResponse> {
    const buildingId = flat.apartmentBuildingId;
    return this.api.post<FlatResponse>(`${this.baseUrl}/${buildingId}/flats`, flat).pipe(
      tap(created => {
        this.notification.success(`Flat "${created.flatNumber}" created successfully`);
        this.invalidateBuildingCache(buildingId);
      })
    );
  }

  /**
   * Update an existing flat
   */
  updateFlat(buildingId: number, flatId: number, flat: FlatRequest): Observable<FlatResponse> {
    return this.api.put<FlatResponse>(`${this.baseUrl}/${buildingId}/flats/${flatId}`, flat).pipe(
      tap(updated => {
        this.notification.success(`Flat "${updated.flatNumber}" updated successfully`);
        this.invalidateBuildingCache(buildingId);
      })
    );
  }

  /**
   * Soft delete a flat (sets isActive = false)
   */
  deleteFlat(buildingId: number, flatId: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${buildingId}/flats/${flatId}`).pipe(
      tap(() => {
        this.notification.success('Flat deactivated successfully');
        this.invalidateBuildingCache(buildingId);
      })
    );
  }

  /**
   * Invalidate cache for a specific building
   */
  private invalidateBuildingCache(buildingId: number): void {
    this.flatsCache.delete(buildingId);
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.flatsCache.clear();
  }

  /**
   * Clean up all polling subscriptions
   */
  ngOnDestroy(): void {
    // Stop all polling
    this.pollingSubjects.forEach((subject, buildingId) => {
      this.stopPolling(buildingId);
    });
  }
}