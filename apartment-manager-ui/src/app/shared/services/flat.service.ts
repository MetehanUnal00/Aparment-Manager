import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Interface representing a Flat entity
 */
export interface Flat {
  id?: number;
  flatNumber: string;
  numberOfRooms?: number;
  areaSqMeters?: number;
  apartmentBuildingId: number;
  apartmentBuildingName?: string;
  tenantName?: string;
  tenantContact?: string;
  tenantEmail?: string;
  monthlyRent?: number;
  securityDeposit?: number;
  tenantMoveInDate?: string;
  isActive?: boolean;
  currentBalance?: number;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Interface for flat financial information
 */
export interface FlatFinancialInfo {
  flat: Flat;
  currentBalance: number;
  totalDebt: number;
  lastPaymentDate?: string;
  lastPaymentAmount?: number;
  upcomingDues: Array<{
    dueDate: string;
    amount: number;
    description: string;
  }>;
  recentPayments: Array<{
    paymentDate: string;
    amount: number;
    description: string;
  }>;
}

/**
 * Service for managing flats
 */
@Injectable({
  providedIn: 'root'
})
export class FlatService {
  /**
   * Base URL for flat API endpoints
   * Note: URL structure includes building ID in the path
   */
  private apiUrl = `${environment.apiUrl}/apartment-buildings`;

  constructor(private http: HttpClient) { }

  /**
   * Get all flats for a specific building
   * @param buildingId The ID of the building
   * @returns Observable of Flat array
   */
  getFlatsByBuilding(buildingId: number): Observable<Flat[]> {
    return this.http.get<Flat[]>(`${this.apiUrl}/${buildingId}/flats`);
  }

  /**
   * Get only active flats for a specific building
   * @param buildingId The ID of the building
   * @returns Observable of Flat array
   */
  getActiveFlatsByBuilding(buildingId: number): Observable<Flat[]> {
    return this.http.get<Flat[]>(`${this.apiUrl}/${buildingId}/flats/active`);
  }

  /**
   * Get a specific flat by ID
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat
   * @returns Observable of Flat
   */
  getFlatById(buildingId: number, flatId: number): Observable<Flat> {
    return this.http.get<Flat>(`${this.apiUrl}/${buildingId}/flats/${flatId}`);
  }

  /**
   * Get flat with financial information
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat
   * @returns Observable of FlatFinancialInfo
   */
  getFlatWithFinancialInfo(buildingId: number, flatId: number): Observable<FlatFinancialInfo> {
    return this.http.get<FlatFinancialInfo>(`${this.apiUrl}/${buildingId}/flats/${flatId}/financial-info`);
  }

  /**
   * Create a new flat
   * @param buildingId The ID of the building
   * @param flat The flat data to create
   * @returns Observable of created Flat
   */
  createFlat(buildingId: number, flat: Flat): Observable<Flat> {
    // Ensure the buildingId in the flat matches the path parameter
    flat.apartmentBuildingId = buildingId;
    return this.http.post<Flat>(`${this.apiUrl}/${buildingId}/flats`, flat);
  }

  /**
   * Update an existing flat
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat to update
   * @param flat The updated flat data
   * @returns Observable of updated Flat
   */
  updateFlat(buildingId: number, flatId: number, flat: Flat): Observable<Flat> {
    // Ensure the buildingId in the flat matches the path parameter
    flat.apartmentBuildingId = buildingId;
    return this.http.put<Flat>(`${this.apiUrl}/${buildingId}/flats/${flatId}`, flat);
  }

  /**
   * Update only tenant information for a flat
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat
   * @param tenantInfo Partial flat data with tenant information
   * @returns Observable of updated Flat
   */
  updateTenantInfo(buildingId: number, flatId: number, tenantInfo: Partial<Flat>): Observable<Flat> {
    return this.http.put<Flat>(`${this.apiUrl}/${buildingId}/flats/${flatId}/tenant`, tenantInfo);
  }

  /**
   * Deactivate a flat (soft delete)
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat to deactivate
   * @returns Observable of updated Flat
   */
  deactivateFlat(buildingId: number, flatId: number): Observable<Flat> {
    return this.http.put<Flat>(`${this.apiUrl}/${buildingId}/flats/${flatId}/deactivate`, null);
  }

  /**
   * Delete a flat (hard delete)
   * @param buildingId The ID of the building
   * @param flatId The ID of the flat to delete
   * @returns Observable of void
   */
  deleteFlat(buildingId: number, flatId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${buildingId}/flats/${flatId}`);
  }

  /**
   * Get vacant flats in a building
   * @param buildingId The ID of the building
   * @returns Observable of Flat array
   */
  getVacantFlats(buildingId: number): Observable<Flat[]> {
    return this.http.get<Flat[]>(`${this.apiUrl}/${buildingId}/flats/vacant`);
  }

  /**
   * Get occupied flats in a building
   * @param buildingId The ID of the building
   * @returns Observable of Flat array
   */
  getOccupiedFlats(buildingId: number): Observable<Flat[]> {
    return this.http.get<Flat[]>(`${this.apiUrl}/${buildingId}/flats/occupied`);
  }

  /**
   * Get flats with overdue payments
   * @param buildingId The ID of the building
   * @returns Observable of Flat array with debt information
   */
  getFlatsWithOverduePayments(buildingId: number): Observable<Array<Flat & { totalDebt: number; overdueCount: number }>> {
    return this.http.get<Array<Flat & { totalDebt: number; overdueCount: number }>>(
      `${this.apiUrl}/${buildingId}/flats/with-overdue`
    );
  }
}