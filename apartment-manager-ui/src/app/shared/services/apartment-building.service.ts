import { Injectable, inject } from '@angular/core';
import { Observable, map, shareReplay, tap } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { 
  ApartmentBuildingResponse, 
  ApartmentBuildingRequest,
  BuildingStatistics,
  BuildingFinancialSummary
} from '../models/apartment-building.model';

/**
 * Service for managing apartment buildings
 * Handles all building-related API operations with caching and error handling
 */
@Injectable({
  providedIn: 'root'
})
export class ApartmentBuildingService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  
  // API endpoints
  private readonly baseUrl = '/apartment-buildings';
  
  // Cache for building list (5 minutes TTL)
  private buildingsCache$?: Observable<ApartmentBuildingResponse[]>;
  private cacheTime?: number;
  private readonly CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

  /**
   * Get all apartment buildings
   * Uses caching to reduce API calls
   */
  getBuildings(forceRefresh = false): Observable<ApartmentBuildingResponse[]> {
    // Check if cache is valid
    if (!forceRefresh && this.buildingsCache$ && this.cacheTime && 
        (Date.now() - this.cacheTime) < this.CACHE_DURATION) {
      return this.buildingsCache$;
    }

    // Create new cached observable
    this.cacheTime = Date.now();
    this.buildingsCache$ = this.api.get<ApartmentBuildingResponse[]>(this.baseUrl).pipe(
      tap(() => console.log('Fetched apartment buildings from API')),
      shareReplay(1) // Share the result and replay for late subscribers
    );

    return this.buildingsCache$;
  }

  /**
   * Get a single apartment building by ID
   */
  getBuilding(id: number): Observable<ApartmentBuildingResponse> {
    return this.api.get<ApartmentBuildingResponse>(`${this.baseUrl}/${id}`).pipe(
      tap(building => console.log(`Fetched building ${id}:`, building))
    );
  }

  /**
   * Create a new apartment building
   */
  createBuilding(building: ApartmentBuildingRequest): Observable<ApartmentBuildingResponse> {
    return this.api.post<ApartmentBuildingResponse>(this.baseUrl, building).pipe(
      tap(created => {
        this.notification.success(`Building "${created.name}" created successfully`);
        this.invalidateCache(); // Clear cache when data changes
      })
    );
  }

  /**
   * Update an existing apartment building
   */
  updateBuilding(id: number, building: ApartmentBuildingRequest): Observable<ApartmentBuildingResponse> {
    return this.api.put<ApartmentBuildingResponse>(`${this.baseUrl}/${id}`, building).pipe(
      tap(updated => {
        this.notification.success(`Building "${updated.name}" updated successfully`);
        this.invalidateCache(); // Clear cache when data changes
      })
    );
  }

  /**
   * Delete an apartment building
   */
  deleteBuilding(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.notification.success('Building deleted successfully');
        this.invalidateCache(); // Clear cache when data changes
      })
    );
  }

  /**
   * Assign a manager to a building
   */
  assignManager(buildingId: number, userId: number): Observable<void> {
    return this.api.post<void>(`${this.baseUrl}/${buildingId}/managers`, { userId }).pipe(
      tap(() => {
        this.notification.success('Manager assigned successfully');
      })
    );
  }

  /**
   * Get building statistics
   * This endpoint might be cached on the backend
   */
  getBuildingStatistics(buildingId: number): Observable<BuildingStatistics> {
    return this.api.get<BuildingStatistics>(`${this.baseUrl}/${buildingId}/statistics`);
  }

  /**
   * Get building financial summary
   */
  getBuildingFinancialSummary(buildingId: number): Observable<BuildingFinancialSummary> {
    return this.api.get<BuildingFinancialSummary>(`${this.baseUrl}/${buildingId}/financial-summary`);
  }

  /**
   * Invalidate the buildings cache
   * Called when data is modified
   */
  private invalidateCache(): void {
    this.buildingsCache$ = undefined;
    this.cacheTime = undefined;
  }
}