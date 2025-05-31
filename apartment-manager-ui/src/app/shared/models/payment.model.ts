/**
 * Payment related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * Payment method enum matching backend
 */
export enum PaymentMethod {
  CASH = 'CASH',
  BANK_TRANSFER = 'BANK_TRANSFER',
  CREDIT_CARD = 'CREDIT_CARD',
  DEBIT_CARD = 'DEBIT_CARD',
  CHECK = 'CHECK',
  ONLINE_PAYMENT = 'ONLINE_PAYMENT',
  OTHER = 'OTHER'
}

/**
 * Flat summary for payment response
 */
export interface FlatSummary {
  id: number;
  flatNumber: string;
  tenantName?: string;
  buildingId: number;
  buildingName: string;
}

/**
 * Payment response interface matching backend PaymentResponse DTO
 */
export interface PaymentResponse {
  id: number;
  flat: FlatSummary;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
  description?: string;
  receiptNumber?: string;
  recordedBy?: string;
  createdAt: string;
  updatedAt?: string;
  version: number;
}

/**
 * Request interface for creating/updating payment
 */
export interface PaymentRequest {
  flatId: number;
  amount: number;
  paymentDate: string;
  paymentMethod: PaymentMethod;
  referenceNumber?: string;
  notes?: string;
  description?: string;
  receiptNumber?: string;
}

/**
 * Payment summary for building
 */
export interface PaymentSummary {
  totalAmount: number;
  paymentCount: number;
  averagePayment: number;
  byMethod: PaymentMethodBreakdown[];
  monthlyTrend: MonthlyPaymentTrend[];
}

/**
 * Payment method breakdown
 */
export interface PaymentMethodBreakdown {
  method: PaymentMethod;
  amount: number;
  count: number;
  percentage: number;
}

/**
 * Monthly payment trend
 */
export interface MonthlyPaymentTrend {
  month: string;
  year: number;
  totalAmount: number;
  paymentCount: number;
}

/**
 * Payment search filters
 */
export interface PaymentSearchFilters {
  buildingId?: number;
  flatId?: number;
  paymentMethod?: PaymentMethod;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
}

/**
 * Payment analytics
 */
export interface PaymentAnalytics {
  totalPayments: number;
  totalAmount: number;
  averagePayment: number;
  paymentsByMonth: MonthlyPaymentTrend[];
  paymentsByMethod: PaymentMethodBreakdown[];
  topPayingFlats: {
    flatId: number;
    flatNumber: string;
    totalPaid: number;
  }[];
}

