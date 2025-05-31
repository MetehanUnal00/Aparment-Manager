import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, timer, switchMap, BehaviorSubject } from 'rxjs';
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
  private readonly baseUrl = '/flats';
  
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
            ? this.api.get<FlatResponse[]>(`${this.baseUrl}/building/${buildingId}`)
            : []
        ),
        shareReplay(1)
      );
    } else {
      // Single fetch with caching
      return this.api.get<FlatResponse[]>(`${this.baseUrl}/building/${buildingId}`).pipe(
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
   */
  getFlat(id: number): Observable<FlatResponse> {
    return this.api.get<FlatResponse>(`${this.baseUrl}/${id}`).pipe(
      tap(flat => console.log(`Fetched flat ${id}:`, flat))
    );
  }

  /**
   * Get flat balance
   * Balance is frequently accessed, cache for 2 minutes
   */
  getFlatBalance(flatId: number): Observable<FlatBalance> {
    return this.api.get<FlatBalance>(`${this.baseUrl}/${flatId}/balance`).pipe(
      // Note: Backend might already cache this with @Cacheable
      tap(balance => console.log(`Flat ${flatId} balance:`, balance.balance))
    );
  }

  /**
   * Get flat financial summary
   */
  getFlatFinancialSummary(flatId: number): Observable<FlatFinancialSummary> {
    return this.api.get<FlatFinancialSummary>(`${this.baseUrl}/${flatId}/financial-summary`);
  }

  /**
   * Create a new flat
   */
  createFlat(flat: FlatRequest): Observable<FlatResponse> {
    return this.api.post<FlatResponse>(this.baseUrl, flat).pipe(
      tap(created => {
        this.notification.success(`Flat "${created.flatNumber}" created successfully`);
        this.invalidateBuildingCache(flat.apartmentBuildingId);
      })
    );
  }

  /**
   * Update an existing flat
   */
  updateFlat(id: number, flat: FlatRequest): Observable<FlatResponse> {
    return this.api.put<FlatResponse>(`${this.baseUrl}/${id}`, flat).pipe(
      tap(updated => {
        this.notification.success(`Flat "${updated.flatNumber}" updated successfully`);
        this.invalidateBuildingCache(updated.apartmentBuildingId);
      })
    );
  }

  /**
   * Soft delete a flat (sets isActive = false)
   */
  deleteFlat(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.notification.success('Flat deactivated successfully');
        // Note: We don't know the building ID here, so clear all caches
        this.flatsCache.clear();
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