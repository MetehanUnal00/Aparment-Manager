/**
 * Dashboard related interfaces and types
 */

import { BuildingStatistics, BuildingFinancialSummary } from './apartment-building.model';
import { DebtorInfo } from './monthly-due.model';
import { ExpenseTrends } from './expense.model';
import { MonthlyPaymentTrend } from './payment.model';

/**
 * Dashboard overview data
 */
export interface DashboardOverview {
  buildingStatistics: BuildingStatistics;
  financialSummary: BuildingFinancialSummary;
  recentDebtors: DebtorInfo[];
  expenseTrends: ExpenseTrends;
  paymentTrends: MonthlyPaymentTrend[];
  alerts: DashboardAlert[];
}

/**
 * Dashboard alert types
 */
export enum AlertType {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  SUCCESS = 'SUCCESS'
}

/**
 * Dashboard alert interface
 */
export interface DashboardAlert {
  type: AlertType;
  title: string;
  message: string;
  timestamp: string;
  actionUrl?: string;
  actionLabel?: string;
}

/**
 * Quick stats for dashboard cards
 */
export interface QuickStats {
  totalRevenue: {
    value: number;
    change: number;
    trend: 'up' | 'down' | 'stable';
  };
  totalExpenses: {
    value: number;
    change: number;
    trend: 'up' | 'down' | 'stable';
  };
  occupancyRate: {
    value: number;
    change: number;
    trend: 'up' | 'down' | 'stable';
  };
  collectionRate: {
    value: number;
    change: number;
    trend: 'up' | 'down' | 'stable';
  };
}

/**
 * Chart data point
 */
export interface ChartDataPoint {
  label: string;
  value: number;
  color?: string;
}

/**
 * Time series data point
 */
export interface TimeSeriesDataPoint {
  date: string;
  value: number;
  label?: string;
}

/**
 * Dashboard filters
 */
export interface DashboardFilters {
  buildingId: number;
  dateRange?: {
    startDate: string;
    endDate: string;
  };
  includeInactive?: boolean;
}