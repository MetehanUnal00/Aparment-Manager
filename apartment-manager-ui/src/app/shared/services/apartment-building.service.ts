import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Interface representing an Apartment Building entity
 */
export interface ApartmentBuilding {
  id?: number;
  name: string;
  address: string;
  numberOfFlats: number;
  numberOfFloors?: number;
  hasElevator?: boolean;
  hasParkingArea?: boolean;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Interface for building statistics
 */
export interface BuildingStatistics {
  totalFlats: number;
  occupiedFlats: number;
  vacantFlats: number;
  totalTenants: number;
  monthlyIncomeTarget: number;
  currentMonthCollection: number;
  totalDebt: number;
  activeManagers: number;
}

/**
 * Service for managing apartment buildings
 */
@Injectable({
  providedIn: 'root'
})
export class ApartmentBuildingService {
  /**
   * Base URL for apartment building API endpoints
   */
  private apiUrl = `${environment.apiUrl}/buildings`;

  constructor(private http: HttpClient) { }

  /**
   * Get all apartment buildings accessible to the current user
   * @returns Observable of ApartmentBuilding array
   */
  getAllBuildings(): Observable<ApartmentBuilding[]> {
    return this.http.get<ApartmentBuilding[]>(this.apiUrl);
  }

  /**
   * Get a specific building by ID
   * @param buildingId The ID of the building
   * @returns Observable of ApartmentBuilding
   */
  getBuildingById(buildingId: number): Observable<ApartmentBuilding> {
    return this.http.get<ApartmentBuilding>(`${this.apiUrl}/${buildingId}`);
  }

  /**
   * Create a new apartment building (Admin only)
   * @param building The building data to create
   * @returns Observable of created ApartmentBuilding
   */
  createBuilding(building: ApartmentBuilding): Observable<ApartmentBuilding> {
    return this.http.post<ApartmentBuilding>(this.apiUrl, building);
  }

  /**
   * Update an existing building
   * @param buildingId The ID of the building to update
   * @param building The updated building data
   * @returns Observable of updated ApartmentBuilding
   */
  updateBuilding(buildingId: number, building: ApartmentBuilding): Observable<ApartmentBuilding> {
    return this.http.put<ApartmentBuilding>(`${this.apiUrl}/${buildingId}`, building);
  }

  /**
   * Delete a building (Admin only)
   * @param buildingId The ID of the building to delete
   * @returns Observable of void
   */
  deleteBuilding(buildingId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${buildingId}`);
  }

  /**
   * Get statistics for a building
   * @param buildingId The ID of the building
   * @returns Observable of BuildingStatistics
   */
  getBuildingStatistics(buildingId: number): Observable<BuildingStatistics> {
    return this.http.get<BuildingStatistics>(`${this.apiUrl}/${buildingId}/statistics`);
  }

  /**
   * Assign a manager to a building (Admin only)
   * @param buildingId The ID of the building
   * @param userId The ID of the user to assign as manager
   * @returns Observable of success message
   */
  assignManager(buildingId: number, userId: number): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/${buildingId}/managers`, { userId });
  }

  /**
   * Remove a manager from a building (Admin only)
   * @param buildingId The ID of the building
   * @param userId The ID of the manager to remove
   * @returns Observable of success message
   */
  removeManager(buildingId: number, userId: number): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.apiUrl}/${buildingId}/managers/${userId}`);
  }

  /**
   * Get list of managers for a building
   * @param buildingId The ID of the building
   * @returns Observable of user array
   */
  getBuildingManagers(buildingId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/${buildingId}/managers`);
  }
}