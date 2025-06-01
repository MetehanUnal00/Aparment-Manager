/**
 * Flat related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * Flat response interface matching backend FlatResponse DTO
 */
export interface FlatResponse {
  id: number;
  flatNumber: string;
  numberOfRooms?: number;
  areaSqMeters?: number;
  apartmentBuildingId: number;
  apartmentBuildingName: string;
  createdAt: string;
  updatedAt: string;
  tenantName?: string;
  tenantContact?: string;
  tenantEmail?: string;
  monthlyRent?: number;
  securityDeposit?: number;
  tenantMoveInDate?: string;
  isActive: boolean;
  currentBalance?: number;
  hasActiveContract?: boolean;
}

/**
 * Request interface for creating/updating flat
 */
export interface FlatRequest {
  flatNumber: string;
  numberOfRooms?: number;
  areaSqMeters?: number;
  apartmentBuildingId: number;
  tenantName?: string;
  tenantContact?: string;
  tenantEmail?: string;
  monthlyRent?: number;
  securityDeposit?: number;
  tenantMoveInDate?: string;
  isActive?: boolean;
}

/**
 * Flat financial summary
 */
export interface FlatFinancialSummary {
  flatId: number;
  flatNumber: string;
  totalPayments: number;
  totalDues: number;
  balance: number;
  lastPaymentDate?: string;
  lastPaymentAmount?: number;
  overdueAmount: number;
  overdueDays: number;
}

/**
 * Flat summary for lists
 */
export interface FlatSummary {
  id: number;
  flatNumber: string;
  floor: number;
  tenantName?: string;
  balance: number;
  isOccupied: boolean;
  monthlyRent: number;
}

/**
 * Flat occupancy history
 */
export interface FlatOccupancyHistory {
  tenantName: string;
  moveInDate: string;
  moveOutDate?: string;
  totalPaid: number;
  totalOwed: number;
}

/**
 * Flat balance information
 */
export interface FlatBalance {
  flatId: number;
  currentBalance: number;
  totalDues: number;
  totalPayments: number;
  lastPaymentDate?: string;
  lastPaymentAmount?: number;
}