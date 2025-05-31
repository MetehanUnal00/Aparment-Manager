import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/**
 * Enum for expense categories
 */
export enum ExpenseCategory {
  MAINTENANCE = 'MAINTENANCE',
  UTILITIES = 'UTILITIES',
  CLEANING = 'CLEANING',
  SECURITY = 'SECURITY',
  ADMINISTRATIVE = 'ADMINISTRATIVE',
  REPAIRS = 'REPAIRS',
  INSURANCE = 'INSURANCE',
  OTHER = 'OTHER'
}

/**
 * Interface representing an Expense entity
 */
export interface Expense {
  id?: number;
  buildingId: number;
  category: ExpenseCategory;
  amount: number;
  description: string;
  expenseDate: string;
  vendor?: string;
  invoiceNumber?: string;
  isRecurring?: boolean;
  recurringPeriod?: string; // MONTHLY, QUARTERLY, YEARLY
  createdAt?: string;
  updatedAt?: string;
}

/**
 * Interface for expense category summary
 */
export interface CategorySummary {
  category: ExpenseCategory;
  totalAmount: number;
  count: number;
  percentage: number;
}

/**
 * Interface for monthly expense summary
 */
export interface MonthlyExpenseSummary {
  month: number;
  year: number;
  totalAmount: number;
  categoryBreakdown: CategorySummary[];
  averagePerFlat: number;
}

/**
 * Interface for expense trends
 */
export interface ExpenseTrend {
  period: string;
  amount: number;
  change: number; // Percentage change from previous period
}

/**
 * Service for managing building expenses
 * Handles expense recording, categorization, and analysis
 */
@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  /**
   * Base URL for expense API endpoints
   */
  private apiUrl = `${environment.apiUrl}/expenses`;

  constructor(private http: HttpClient) { }

  /**
   * Get all expenses for a building
   * @param buildingId The ID of the building
   * @param startDate Optional start date filter
   * @param endDate Optional end date filter
   * @param category Optional category filter
   * @returns Observable of Expense array
   */
  getExpensesByBuilding(
    buildingId: number, 
    startDate?: string, 
    endDate?: string, 
    category?: ExpenseCategory
  ): Observable<Expense[]> {
    let params = new HttpParams();
    
    // Add optional filters if provided
    if (startDate) {
      params = params.append('startDate', startDate);
    }
    if (endDate) {
      params = params.append('endDate', endDate);
    }
    if (category) {
      params = params.append('category', category);
    }

    return this.http.get<Expense[]>(`${this.apiUrl}/building/${buildingId}`, { params });
  }

  /**
   * Get a specific expense by ID
   * @param expenseId The ID of the expense
   * @returns Observable of Expense
   */
  getExpenseById(expenseId: number): Observable<Expense> {
    return this.http.get<Expense>(`${this.apiUrl}/${expenseId}`);
  }

  /**
   * Create a new expense
   * @param expense The expense data to create
   * @returns Observable of created Expense
   */
  createExpense(expense: Expense): Observable<Expense> {
    return this.http.post<Expense>(this.apiUrl, expense);
  }

  /**
   * Update an existing expense
   * @param expenseId The ID of the expense to update
   * @param expense The updated expense data
   * @returns Observable of updated Expense
   */
  updateExpense(expenseId: number, expense: Expense): Observable<Expense> {
    return this.http.put<Expense>(`${this.apiUrl}/${expenseId}`, expense);
  }

  /**
   * Delete an expense
   * @param expenseId The ID of the expense to delete
   * @returns Observable of void
   */
  deleteExpense(expenseId: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${expenseId}`);
  }

  /**
   * Get expense summary by category for a building
   * @param buildingId The ID of the building
   * @param startDate Optional start date filter
   * @param endDate Optional end date filter
   * @returns Observable of CategorySummary array
   */
  getCategorySummary(
    buildingId: number, 
    startDate?: string, 
    endDate?: string
  ): Observable<CategorySummary[]> {
    let params = new HttpParams();
    
    // Add optional filters if provided
    if (startDate) {
      params = params.append('startDate', startDate);
    }
    if (endDate) {
      params = params.append('endDate', endDate);
    }

    return this.http.get<CategorySummary[]>(`${this.apiUrl}/category-summary/building/${buildingId}`, { params });
  }

  /**
   * Get monthly expense summary
   * @param buildingId The ID of the building
   * @param year The year to get summary for
   * @returns Observable of MonthlyExpenseSummary array
   */
  getMonthlyExpenseSummary(buildingId: number, year: number): Observable<MonthlyExpenseSummary[]> {
    const params = new HttpParams().append('year', year.toString());
    return this.http.get<MonthlyExpenseSummary[]>(`${this.apiUrl}/monthly-summary/building/${buildingId}`, { params });
  }

  /**
   * Get expense trends over time
   * @param buildingId The ID of the building
   * @param period Period type: 'monthly', 'quarterly', 'yearly'
   * @param count Number of periods to analyze
   * @returns Observable of ExpenseTrend array
   */
  getExpenseTrends(buildingId: number, period: string, count: number): Observable<ExpenseTrend[]> {
    const params = new HttpParams()
      .append('period', period)
      .append('count', count.toString());

    return this.http.get<ExpenseTrend[]>(`${this.apiUrl}/trends/building/${buildingId}`, { params });
  }

  /**
   * Get recurring expenses for a building
   * @param buildingId The ID of the building
   * @returns Observable of Expense array
   */
  getRecurringExpenses(buildingId: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.apiUrl}/recurring/building/${buildingId}`);
  }

  /**
   * Calculate expense per flat for a given period
   * @param buildingId The ID of the building
   * @param month The month
   * @param year The year
   * @returns Observable of number (amount per flat)
   */
  getExpensePerFlat(buildingId: number, month: number, year: number): Observable<number> {
    const params = new HttpParams()
      .append('month', month.toString())
      .append('year', year.toString());

    return this.http.get<number>(`${this.apiUrl}/per-flat/building/${buildingId}`, { params });
  }

  /**
   * Batch create multiple expenses
   * @param expenses Array of expenses to create
   * @returns Observable of Expense array
   */
  createBatchExpenses(expenses: Expense[]): Observable<Expense[]> {
    return this.http.post<Expense[]>(`${this.apiUrl}/batch`, expenses);
  }

  /**
   * Get top vendors by expense amount
   * @param buildingId The ID of the building
   * @param limit Number of vendors to return
   * @returns Observable of vendor summary array
   */
  getTopVendors(buildingId: number, limit: number = 10): Observable<{ vendor: string; totalAmount: number; count: number }[]> {
    const params = new HttpParams().append('limit', limit.toString());
    return this.http.get<{ vendor: string; totalAmount: number; count: number }[]>(
      `${this.apiUrl}/top-vendors/building/${buildingId}`, 
      { params }
    );
  }
}