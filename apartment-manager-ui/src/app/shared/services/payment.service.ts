import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Interface representing a Payment entity
 */
export interface Payment {
  id?: number;
  flatId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: string;
  description?: string;
  createdAt?: string;
}

/**
 * Interface for payment statistics
 */
export interface PaymentStatistics {
  totalAmount: number;
  totalCount: number;
  averageAmount: number;
  lastPaymentDate?: string;
}

/**
 * Service for managing apartment payments
 * Handles all payment-related operations including recording payments,
 * retrieving payment history, and calculating statistics
 */
@Injectable({
  providedIn: 'root'
})
export class PaymentService {
  /**
   * Base URL for payment API endpoints
   */
  private apiUrl = `${environment.apiUrl}/payments`;

  constructor(private http: HttpClient) { }

  /**
   * Get all payments for a specific flat
   * @param flatId The ID of the flat
   * @param startDate Optional start date filter
   * @param endDate Optional end date filter
   * @returns Observable of Payment array
   */
  getPaymentsByFlat(flatId: number, startDate?: string, endDate?: string): Observable<Payment[]> {
    let params = new HttpParams();
    
    // Add optional date filters if provided
    if (startDate) {
      params = params.append('startDate', startDate);
    }
    if (endDate) {
      params = params.append('endDate', endDate);
    }

    return this.http.get<Payment[]>(`${this.apiUrl}/flat/${flatId}`, { params });
  }

  /**
   * Get all payments for a specific building
   * @param buildingId The ID of the building
   * @param startDate Optional start date filter
   * @param endDate Optional end date filter
   * @returns Observable of Payment array
   */
  getPaymentsByBuilding(buildingId: number, startDate?: string, endDate?: string): Observable<Payment[]> {
    let params = new HttpParams();
    
    // Add optional date filters if provided
    if (startDate) {
      params = params.append('startDate', startDate);
    }
    if (endDate) {
      params = params.append('endDate', endDate);
    }

    return this.http.get<Payment[]>(`${this.apiUrl}/building/${buildingId}`, { params });
  }

  /**
   * Get a specific payment by ID
   * @param paymentId The ID of the payment
   * @returns Observable of Payment
   */
  getPaymentById(paymentId: number): Observable<Payment> {
    return this.http.get<Payment>(`${this.apiUrl}/${paymentId}`);
  }

  /**
   * Record a new payment
   * @param payment The payment data to record
   * @returns Observable of created Payment
   */
  createPayment(payment: Payment): Observable<Payment> {
    return this.http.post<Payment>(this.apiUrl, payment);
  }

  /**
   * Update an existing payment
   * @param paymentId The ID of the payment to update
   * @param payment The updated payment data
   * @returns Observable of updated Payment
   */
  updatePayment(paymentId: number, payment: Payment): Observable<Payment> {
    return this.http.put<Payment>(`${this.apiUrl}/${paymentId}`, payment);
  }

  /**
   * Delete a payment
   * @param paymentId The ID of the payment to delete
   * @returns Observable of void
   */
  deletePayment(paymentId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${paymentId}`);
  }

  /**
   * Get payment statistics for a building
   * @param buildingId The ID of the building
   * @param startDate Optional start date filter
   * @param endDate Optional end date filter
   * @returns Observable of PaymentStatistics
   */
  getPaymentStatistics(buildingId: number, startDate?: string, endDate?: string): Observable<PaymentStatistics> {
    let params = new HttpParams();
    
    // Add optional date filters if provided
    if (startDate) {
      params = params.append('startDate', startDate);
    }
    if (endDate) {
      params = params.append('endDate', endDate);
    }

    return this.http.get<PaymentStatistics>(`${this.apiUrl}/statistics/building/${buildingId}`, { params });
  }

  /**
   * Calculate outstanding balance for a flat
   * @param flatId The ID of the flat
   * @returns Observable of number (balance amount)
   */
  getOutstandingBalance(flatId: number): Observable<number> {
    return this.http.get<number>(`${this.apiUrl}/balance/flat/${flatId}`);
  }

  /**
   * Batch record multiple payments
   * @param payments Array of payments to record
   * @returns Observable of Payment array
   */
  createBatchPayments(payments: Payment[]): Observable<Payment[]> {
    return this.http.post<Payment[]>(`${this.apiUrl}/batch`, payments);
  }
}