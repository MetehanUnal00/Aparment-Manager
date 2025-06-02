/**
 * Flat related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * Active contract information embedded in FlatResponse
 */
export interface ActiveContractInfo {
  contractId: number;
  tenantName?: string;
  tenantEmail?: string;
  tenantContact?: string;
  monthlyRent: number;
  securityDeposit: number;
  startDate: string;
  endDate: string;
  moveInDate: string;
  daysUntilExpiry?: number;
  isExpiringSoon: boolean;
  contractStatus: string;
  outstandingBalance?: number;
  hasOverdueDues?: boolean;
}

/**
 * Occupancy summary for historical data
 */
export interface OccupancySummary {
  totalContracts: number;
  firstOccupancyDate?: string;
  lastVacancyDate?: string;
  averageRent: number;
  totalMonthsOccupied: number;
}

/**
 * Occupancy status enum
 */
export type OccupancyStatus = 'OCCUPIED' | 'VACANT' | 'PENDING_MOVE_IN';

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
  isActive: boolean;
  currentBalance?: number;
  
  // Contract information
  activeContract?: ActiveContractInfo;
  occupancyStatus: OccupancyStatus;
  occupancySummary?: OccupancySummary;
}

/**
 * Request interface for creating/updating flat
 * Note: Tenant information is now managed through contracts
 */
export interface FlatRequest {
  flatNumber: string;
  numberOfRooms?: number;
  areaSqMeters?: number;
  apartmentBuildingId: number;
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