/**
 * Expense related interfaces and types
 */

import { BaseEntity } from './common.model';

/**
 * Expense category enum matching backend
 */
export enum ExpenseCategory {
  MAINTENANCE = 'MAINTENANCE',
  UTILITIES = 'UTILITIES',
  CLEANING = 'CLEANING',
  SECURITY = 'SECURITY',
  INSURANCE = 'INSURANCE',
  TAXES = 'TAXES',
  MANAGEMENT = 'MANAGEMENT',
  REPAIRS = 'REPAIRS',
  LANDSCAPING = 'LANDSCAPING',
  ELEVATOR = 'ELEVATOR',
  SUPPLIES = 'SUPPLIES',
  LEGAL = 'LEGAL',
  ACCOUNTING = 'ACCOUNTING',
  MARKETING = 'MARKETING',
  OTHER = 'OTHER'
}

/**
 * Recurrence frequency enum matching backend
 */
export enum RecurrenceFrequency {
  WEEKLY = 'WEEKLY',
  MONTHLY = 'MONTHLY',
  QUARTERLY = 'QUARTERLY',
  SEMI_ANNUAL = 'SEMI_ANNUAL',
  ANNUAL = 'ANNUAL'
}

/**
 * Building summary for expense response
 * Used specifically in expense response
 */
export interface ExpenseBuildingSummary {
  id: number;
  name: string;
  address: string;
}

/**
 * Expense response interface matching backend ExpenseResponse DTO
 */
export interface ExpenseResponse {
  id: number;
  building: ExpenseBuildingSummary;
  category: ExpenseCategory;
  categoryDisplayName: string;
  amount: number;
  expenseDate: string;
  description: string;
  vendorName?: string;
  invoiceNumber?: string;
  isRecurring: boolean;
  recurrenceFrequency?: RecurrenceFrequency;
  recordedBy?: string;
  createdAt: string;
  updatedAt?: string;
}

/**
 * Request interface for creating/updating expense
 */
export interface ExpenseRequest {
  buildingId: number;
  category: ExpenseCategory;
  amount: number;
  expenseDate: string;
  description: string;
  vendorName?: string;
  invoiceNumber?: string;
  isRecurring?: boolean;
  recurrenceFrequency?: RecurrenceFrequency;
  distributeToFlats?: boolean;
}

/**
 * Expense summary by category
 */
export interface ExpenseCategorySummary {
  category: ExpenseCategory;
  totalAmount: number;
  count: number;
  percentage: number;
  averageAmount: number;
}

/**
 * Monthly expense summary
 */
export interface MonthlyExpenseSummary {
  month: string;
  year: number;
  totalAmount: number;
  expenseCount: number;
  byCategory: ExpenseCategorySummary[];
}

/**
 * Expense trends
 */
export interface ExpenseTrends {
  currentMonth: number;
  previousMonth: number;
  monthlyAverage: number;
  yearToDate: number;
  changePercentage: number;
  topCategories: ExpenseCategorySummary[];
}

/**
 * Expense search filters
 */
export interface ExpenseSearchFilters {
  buildingId?: number;
  category?: ExpenseCategory;
  startDate?: string;
  endDate?: string;
  minAmount?: number;
  maxAmount?: number;
  vendorName?: string;
  isRecurring?: boolean;
}

