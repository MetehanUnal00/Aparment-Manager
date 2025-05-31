import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Enum for due status
 */
export enum DueStatus {
  PENDING = 'PENDING',
  PAID = 'PAID',
  OVERDUE = 'OVERDUE',
  CANCELLED = 'CANCELLED'
}

/**
 * Interface representing a Monthly Due entity
 */
export interface MonthlyDue {
  id?: number;
  flatId: number;
  dueAmount: number;
  dueDate: string;
  status: DueStatus;
  dueDescription?: string;
  paidAmount?: number;
  paymentDate?: string;
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Interface for debtor information
 */
export interface DebtorInfo {
  flatId: number;
  flatNumber: string;
  tenantName?: string;
  totalDebt: number;
  overdueCount: number;
  oldestDueDate: string;
  daysOverdue: number;
}

/**
 * Interface for collection rate statistics
 */
export interface CollectionRate {
  totalDues: number;
  paidDues: number;
  pendingDues: number;
  overdueDues: number;
  collectionRate: number;
  totalDueAmount: number;
  totalPaidAmount: number;
}

/**
 * Service for managing monthly dues
 * Handles due generation, tracking, overdue management, and collection statistics
 */
@Injectable({
  providedIn: 'root'
})
export class MonthlyDueService {
  /**
   * Base URL for monthly due API endpoints
   */
  private apiUrl = `${environment.apiUrl}/monthly-dues`;

  constructor(private http: HttpClient) { }

  /**
   * Get all monthly dues for a specific flat
   * @param flatId The ID of the flat
   * @param status Optional status filter
   * @returns Observable of MonthlyDue array
   */
  getDuesByFlat(flatId: number, status?: DueStatus): Observable<MonthlyDue[]> {
    let params = new HttpParams();
    
    // Add optional status filter if provided
    if (status) {
      params = params.append('status', status);
    }

    return this.http.get<MonthlyDue[]>(`${this.apiUrl}/flat/${flatId}`, { params });
  }

  /**
   * Get all overdue payments for a building
   * @param buildingId The ID of the building
   * @returns Observable of MonthlyDue array
   */
  getOverdueDuesByBuilding(buildingId: number): Observable<MonthlyDue[]> {
    return this.http.get<MonthlyDue[]>(`${this.apiUrl}/overdue/building/${buildingId}`);
  }

  /**
   * Get debtors list with summary information
   * @param buildingId The ID of the building
   * @returns Observable of DebtorInfo array
   */
  getDebtorsList(buildingId: number): Observable<DebtorInfo[]> {
    return this.http.get<DebtorInfo[]>(`${this.apiUrl}/debtors/building/${buildingId}`);
  }

  /**
   * Generate monthly dues for all flats in a building
   * @param buildingId The ID of the building
   * @param month The month (1-12)
   * @param year The year
   * @returns Observable of MonthlyDue array
   */
  generateMonthlyDues(buildingId: number, month: number, year: number): Observable<MonthlyDue[]> {
    const params = new HttpParams()
      .append('month', month.toString())
      .append('year', year.toString());

    return this.http.post<MonthlyDue[]>(`${this.apiUrl}/generate/building/${buildingId}`, null, { params });
  }

  /**
   * Get a specific monthly due by ID
   * @param dueId The ID of the monthly due
   * @returns Observable of MonthlyDue
   */
  getDueById(dueId: number): Observable<MonthlyDue> {
    return this.http.get<MonthlyDue>(`${this.apiUrl}/${dueId}`);
  }

  /**
   * Mark a monthly due as paid
   * @param dueId The ID of the monthly due
   * @param paymentInfo Payment information (amount and date)
   * @returns Observable of updated MonthlyDue
   */
  markAsPaid(dueId: number, paymentInfo: { paidAmount: number; paymentDate: string }): Observable<MonthlyDue> {
    return this.http.put<MonthlyDue>(`${this.apiUrl}/${dueId}/pay`, paymentInfo);
  }

  /**
   * Update a monthly due
   * @param dueId The ID of the monthly due
   * @param due The updated due data
   * @returns Observable of updated MonthlyDue
   */
  updateDue(dueId: number, due: MonthlyDue): Observable<MonthlyDue> {
    return this.http.put<MonthlyDue>(`${this.apiUrl}/${dueId}`, due);
  }

  /**
   * Cancel a monthly due
   * @param dueId The ID of the monthly due
   * @returns Observable of updated MonthlyDue
   */
  cancelDue(dueId: number): Observable<MonthlyDue> {
    return this.http.put<MonthlyDue>(`${this.apiUrl}/${dueId}/cancel`, null);
  }

  /**
   * Get collection rate statistics for a building
   * @param buildingId The ID of the building
   * @param month Optional month filter
   * @param year Optional year filter
   * @returns Observable of CollectionRate
   */
  getCollectionRate(buildingId: number, month?: number, year?: number): Observable<CollectionRate> {
    let params = new HttpParams();
    
    // Add optional filters if provided
    if (month) {
      params = params.append('month', month.toString());
    }
    if (year) {
      params = params.append('year', year.toString());
    }

    return this.http.get<CollectionRate>(`${this.apiUrl}/collection-rate/building/${buildingId}`, { params });
  }

  /**
   * Get total outstanding debt for a flat
   * @param flatId The ID of the flat
   * @returns Observable of number (total debt amount)
   */
  getTotalDebt(flatId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/debt/flat/${flatId}`);
  }

  /**
   * Get unpaid dues count for a building
   * @param buildingId The ID of the building
   * @returns Observable of number (count of unpaid dues)
   */
  getUnpaidDuesCount(buildingId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/unpaid-count/building/${buildingId}`);
  }

  /**
   * Send overdue notifications for a building
   * @param buildingId The ID of the building
   * @returns Observable of notification results
   */
  sendOverdueNotifications(buildingId: number): Observable<{ sentCount: number; failedCount: number }> {
    return this.http.post<{ sentCount: number; failedCount: number }>(
      `${this.apiUrl}/notify-overdue/building/${buildingId}`, 
      null
    );
  }
}