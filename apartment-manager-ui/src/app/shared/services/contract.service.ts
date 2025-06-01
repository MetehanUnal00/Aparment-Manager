import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, timer, switchMap, BehaviorSubject, map, of } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { 
  ContractRequest,
  ContractResponse,
  ContractSummaryResponse,
  ContractRenewalRequest,
  ContractCancellationRequest,
  ContractModificationRequest,
  ContractExpiryNotification,
  ContractStatistics,
  ContractSearchParams,
  ContractStatus
} from '../models/contract.model';
import { PaginationParams, PaginatedResponse } from '../models/common.model';

/**
 * Service for managing contracts
 * Handles all contract-related API operations with caching and polling support
 */
@Injectable({
  providedIn: 'root'
})
export class ContractService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  
  // API endpoints
  private readonly baseUrl = '/contracts';
  
  // Cache for contract lists (key: serialized params)
  private contractListCache = new Map<string, {
    data$: Observable<PaginatedResponse<ContractSummaryResponse>>;
    time: number;
  }>();
  
  // Cache for contract details (key: contractId)
  private contractDetailCache = new Map<number, {
    data$: Observable<ContractResponse>;
    time: number;
  }>();
  
  // Cache durations
  private readonly LIST_CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
  private readonly DETAIL_CACHE_DURATION = 15 * 60 * 1000; // 15 minutes
  
  // Polling subjects for auto-refresh
  private pollingSubjects = new Map<string, BehaviorSubject<boolean>>();

  /**
   * Create a new contract
   */
  createContract(contract: ContractRequest): Observable<ContractResponse> {
    return this.api.post<ContractResponse>(this.baseUrl, contract).pipe(
      tap(created => {
        this.notification.success(`Contract for flat ${created.flatNumber} created successfully`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Get contract by ID
   * Caches for 15 minutes
   */
  getContractById(id: number, forceRefresh = false): Observable<ContractResponse> {
    // Check cache validity
    const cached = this.contractDetailCache.get(id);
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.DETAIL_CACHE_DURATION) {
      return cached.data$;
    }

    // Create new observable
    const contract$ = this.api.get<ContractResponse>(`${this.baseUrl}/${id}`).pipe(
      tap(contract => console.log(`Fetched contract ${id}:`, contract)),
      shareReplay(1)
    );
    
    // Cache the observable
    this.contractDetailCache.set(id, {
      data$: contract$,
      time: Date.now()
    });

    return contract$;
  }

  /**
   * Get contracts by flat ID
   * Returns contract history for a flat
   */
  getContractsByFlatId(flatId: number): Observable<ContractSummaryResponse[]> {
    return this.api.get<ContractSummaryResponse[]>(`${this.baseUrl}/flat/${flatId}`).pipe(
      tap(contracts => console.log(`Fetched ${contracts.length} contracts for flat ${flatId}`))
    );
  }

  /**
   * Get active contract for a flat
   */
  getActiveContractByFlatId(flatId: number): Observable<ContractResponse> {
    return this.api.get<ContractResponse>(`${this.baseUrl}/flat/${flatId}/active`).pipe(
      tap(contract => console.log(`Fetched active contract for flat ${flatId}:`, contract))
    );
  }

  /**
   * Get contracts by building with pagination
   * Supports caching and optional polling
   */
  getContractsByBuilding(
    buildingId: number, 
    pageRequest: PaginationParams = { page: 0, size: 10 },
    options: { forceRefresh?: boolean; enablePolling?: boolean } = {}
  ): Observable<PaginatedResponse<ContractSummaryResponse>> {
    const { forceRefresh = false, enablePolling = false } = options;
    const cacheKey = `building-${buildingId}-${JSON.stringify(pageRequest)}`;

    // Check cache validity
    const cached = this.contractListCache.get(cacheKey);
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.LIST_CACHE_DURATION) {
      return cached.data$;
    }

    // Create new observable
    const contracts$ = this.createContractsObservable(
      `${this.baseUrl}/building/${buildingId}`,
      this.buildHttpParams(pageRequest),
      cacheKey,
      enablePolling
    );
    
    // Cache the observable
    this.contractListCache.set(cacheKey, {
      data$: contracts$,
      time: Date.now()
    });

    return contracts$;
  }

  /**
   * Search contracts by tenant name
   */
  searchContracts(
    searchParams: ContractSearchParams,
    pageRequest: PaginationParams = { page: 0, size: 10 }
  ): Observable<PaginatedResponse<ContractSummaryResponse>> {
    // The backend requires a 'search' parameter, so we need to ensure it's provided
    const searchTerm = searchParams.tenantName || '';
    const params = this.buildHttpParams({ 
      search: searchTerm,
      page: pageRequest.page,
      size: pageRequest.size
    });
    return this.api.get<PaginatedResponse<ContractSummaryResponse>>(`${this.baseUrl}/search`, params).pipe(
      tap(result => console.log(`Search found ${result.totalElements} contracts`))
    );
  }

  /**
   * Get expiring contracts
   */
  getExpiringContracts(days: number = 30): Observable<ContractSummaryResponse[]> {
    const params = this.buildHttpParams({ days });
    return this.api.get<ContractSummaryResponse[]>(`${this.baseUrl}/expiring`, params).pipe(
      tap(contracts => console.log(`Found ${contracts.length} contracts expiring within ${days} days`))
    );
  }

  /**
   * Get contracts with overdue payments
   */
  getOverdueContracts(): Observable<ContractSummaryResponse[]> {
    return this.api.get<ContractSummaryResponse[]>(`${this.baseUrl}/overdue`).pipe(
      tap(contracts => console.log(`Found ${contracts.length} contracts with overdue payments`))
    );
  }

  /**
   * Get renewable contracts
   */
  getRenewableContracts(days: number = 30): Observable<ContractSummaryResponse[]> {
    const params = this.buildHttpParams({ days });
    return this.api.get<ContractSummaryResponse[]>(`${this.baseUrl}/renewable`, params).pipe(
      tap(contracts => console.log(`Found ${contracts.length} renewable contracts`))
    );
  }

  /**
   * Renew a contract
   */
  renewContract(id: number, request: ContractRenewalRequest): Observable<ContractResponse> {
    return this.api.post<ContractResponse>(`${this.baseUrl}/${id}/renew`, request).pipe(
      tap(renewed => {
        this.notification.success(`Contract renewed successfully. New contract ID: ${renewed.id}`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Cancel a contract
   */
  cancelContract(id: number, request: ContractCancellationRequest): Observable<ContractResponse> {
    return this.api.post<ContractResponse>(`${this.baseUrl}/${id}/cancel`, request).pipe(
      tap(cancelled => {
        this.notification.success(`Contract ${id} cancelled successfully`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Modify a contract
   */
  modifyContract(id: number, request: ContractModificationRequest): Observable<ContractResponse> {
    return this.api.post<ContractResponse>(`${this.baseUrl}/${id}/modify`, request).pipe(
      tap(modified => {
        this.notification.success(`Contract modified successfully. New contract ID: ${modified.id}`);
        this.invalidateCache();
      })
    );
  }

  /**
   * Get contract statistics for a building
   */
  getContractStatistics(buildingId: number): Observable<ContractStatistics> {
    return this.api.get<ContractStatistics>(`${this.baseUrl}/building/${buildingId}/statistics`).pipe(
      tap(stats => console.log(`Contract statistics for building ${buildingId}:`, stats))
    );
  }

  /**
   * Get total monthly rent for a building
   */
  getTotalMonthlyRent(buildingId: number): Observable<number> {
    return this.api.get<{ totalRent: number }>(`${this.baseUrl}/building/${buildingId}/monthly-rent`).pipe(
      map(response => response.totalRent),
      tap(total => console.log(`Total monthly rent for building ${buildingId}: ${total}`))
    );
  }

  /**
   * Generate expiry notifications (Admin only)
   */
  generateExpiryNotifications(): Observable<ContractExpiryNotification[]> {
    return this.api.post<ContractExpiryNotification[]>(`${this.baseUrl}/notifications/expiry`, {}).pipe(
      tap(notifications => {
        this.notification.success(`Generated ${notifications.length} expiry notifications`);
      })
    );
  }

  /**
   * Update contract statuses (Admin only)
   */
  updateContractStatuses(): Observable<void> {
    return this.api.post<void>(`${this.baseUrl}/update-statuses`, {}).pipe(
      tap(() => {
        this.notification.success('Contract statuses updated successfully');
        this.invalidateCache();
      })
    );
  }

  /**
   * Create observable for contracts with optional polling
   */
  private createContractsObservable(
    url: string,
    params: any,
    cacheKey: string,
    enablePolling: boolean
  ): Observable<PaginatedResponse<ContractSummaryResponse>> {
    if (enablePolling) {
      // Get or create polling subject
      let pollingSubject = this.pollingSubjects.get(cacheKey);
      if (!pollingSubject) {
        pollingSubject = new BehaviorSubject<boolean>(true);
        this.pollingSubjects.set(cacheKey, pollingSubject);
      }

      // Poll every 30 seconds when active
      return timer(0, 30000).pipe(
        switchMap(() => pollingSubject.asObservable()),
        switchMap(isActive => 
          isActive 
            ? this.api.get<PaginatedResponse<ContractSummaryResponse>>(url, params)
            : of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 0, first: true, last: true, empty: true } as PaginatedResponse<ContractSummaryResponse>)
        ),
        shareReplay(1)
      );
    } else {
      // Single fetch with caching
      return this.api.get<PaginatedResponse<ContractSummaryResponse>>(url, params).pipe(
        tap(result => console.log(`Fetched ${result.content.length} contracts`)),
        shareReplay(1)
      );
    }
  }

  /**
   * Stop polling for a specific cache key
   */
  stopPolling(cacheKey: string): void {
    const subject = this.pollingSubjects.get(cacheKey);
    if (subject) {
      subject.next(false);
      subject.complete();
      this.pollingSubjects.delete(cacheKey);
    }
  }

  /**
   * Stop all polling
   */
  stopAllPolling(): void {
    this.pollingSubjects.forEach((subject, key) => {
      subject.next(false);
      subject.complete();
    });
    this.pollingSubjects.clear();
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.contractListCache.clear();
    this.contractDetailCache.clear();
    console.log('Contract cache cleared');
  }
  
  /**
   * Check if dates overlap with existing contracts for a flat
   */
  checkOverlap(flatId: number, startDate: string, endDate: string, excludeId?: number): Observable<boolean> {
    // Create HttpParams for the request
    let params = new HttpParams()
      .set('flatId', flatId.toString())
      .set('startDate', startDate)
      .set('endDate', endDate);
    
    if (excludeId) {
      params = params.set('excludeId', excludeId.toString());
    }
    
    return this.api.get<boolean>(`${this.baseUrl}/check-overlap`, params);
  }

  /**
   * Invalidate cache after mutations
   */
  private invalidateCache(): void {
    // Clear all list caches as they might be affected
    this.contractListCache.clear();
    // Keep detail cache but mark as stale by clearing time
    this.contractDetailCache.forEach((cache, key) => {
      cache.time = 0;
    });
  }

  /**
   * Refresh a specific contract
   */
  refreshContract(id: number): Observable<ContractResponse> {
    return this.getContractById(id, true);
  }

  /**
   * Check if a flat has an active contract
   * Used for UI validations
   */
  hasActiveContract(flatId: number): Observable<boolean> {
    return this.getActiveContractByFlatId(flatId).pipe(
      map(() => true),
      tap(hasContract => console.log(`Flat ${flatId} has active contract: ${hasContract}`))
    );
  }

  /**
   * Build HttpParams from object
   * Filters out null/undefined values
   */
  private buildHttpParams(params: any): HttpParams {
    let httpParams = new HttpParams();
    
    Object.keys(params).forEach(key => {
      const value = params[key];
      if (value !== null && value !== undefined) {
        httpParams = httpParams.set(key, value.toString());
      }
    });
    
    return httpParams;
  }
}