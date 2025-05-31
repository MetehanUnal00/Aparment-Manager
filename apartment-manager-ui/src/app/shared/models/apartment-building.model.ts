/**
 * Apartment Building related interfaces and types
 */

import { BaseEntity } from './common.model';
import { UserSummary } from './user.model';

/**
 * Apartment Building response interface matching backend
 */
export interface ApartmentBuildingResponse {
  id: number;
  name: string;
  address: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * Request interface for creating/updating apartment building
 */
export interface ApartmentBuildingRequest {
  name: string;
  address?: string;
}

/**
 * Building statistics interface
 */
export interface BuildingStatistics {
  totalFlats: number;
  occupiedFlats: number;
  vacantFlats: number;
  totalTenants: number;
  monthlyIncomeTarget: number;
  currentMonthCollection: number;
  totalDebt: number;
  debtorCount: number;
  activeManagers: number;
}

/**
 * Building financial summary
 */
export interface BuildingFinancialSummary {
  totalRevenue: number;
  totalExpenses: number;
  netIncome: number;
  outstandingDues: number;
  collectionRate: number;
  averagePaymentDelay: number;
}

/**
 * Building summary for lists
 */
export interface BuildingSummary {
  id: number;
  name: string;
  address: string;
  numberOfFlats: number;
  occupancyRate: number;
  monthlyRevenue: number;
}