/**
 * Monthly Due related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * Due status enum matching backend
 */
export enum DueStatus {
  UNPAID = 'UNPAID',
  PAID = 'PAID',
  PARTIALLY_PAID = 'PARTIALLY_PAID',
  OVERDUE = 'OVERDUE',
  WAIVED = 'WAIVED',
  CANCELLED = 'CANCELLED'
}

/**
 * Flat summary for monthly due response
 */
export interface FlatSummaryForDue {
  id: number;
  flatNumber: string;
  tenantName?: string;
  tenantContact?: string;
}

/**
 * Monthly Due response interface matching backend MonthlyDueResponse DTO
 */
export interface MonthlyDueResponse {
  id: number;
  flat: FlatSummaryForDue;
  dueAmount: number;
  dueDate: string;
  status: DueStatus;
  dueDescription?: string;
  paidAmount?: number;
  paymentDate?: string;
  paidDate?: string;
  baseRent?: number;
  additionalCharges?: number;
  additionalChargesDescription?: string;
  isOverdue: boolean;
  createdAt: string;
  updatedAt?: string;
}

/**
 * Request interface for creating/updating monthly due
 */
export interface MonthlyDueRequest {
  flatId?: number;
  buildingId?: number;
  dueAmount: number;
  dueDate: string;
  dueDescription?: string;
  baseRent?: number;
  additionalCharges?: number;
  additionalChargesDescription?: string;
  useFlatsMonthlyRent?: boolean;
  fallbackAmount?: number;
}

/**
 * Debtor information interface
 */
export interface DebtorInfo {
  flatId: number;
  flatNumber: string;
  tenantName: string;
  tenantContact?: string;
  tenantEmail?: string;
  totalDebt: number;
  oldestUnpaidDueDate: string;
  unpaidDuesCount: number;
  overdueDays: number;
  monthlyRent: number;
}

/**
 * Debtor list summary
 */
export interface DebtorListSummary {
  totalDebtors: number;
  totalDebtAmount: number;
  averageDebtPerDebtor: number;
  debtors: DebtorInfo[];
}

/**
 * Monthly due generation request
 */
export interface MonthlyDueGenerationRequest {
  buildingId: number;
  year: number;
  month: number;
  useDefaultAmount?: boolean;
  customAmount?: number;
  skipOccupiedCheck?: boolean;
  description?: string;
}

/**
 * Monthly due generation result
 */
export interface MonthlyDueGenerationResult {
  totalGenerated: number;
  totalAmount: number;
  skippedFlats: number;
  errors: string[];
}

/**
 * Monthly due filters
 */
export interface MonthlyDueFilters {
  buildingId?: number;
  flatId?: number;
  year?: number;
  month?: number;
  status?: DueStatus;
  overdueOnly?: boolean;
}

/**
 * Overdue summary for dashboard
 */
export interface OverdueSummary {
  dueId: number;
  flatId: number;
  flatNumber: string;
  tenantName?: string;
  dueAmount: number;
  dueDate: string;
  daysOverdue: number;
  description?: string;
}

