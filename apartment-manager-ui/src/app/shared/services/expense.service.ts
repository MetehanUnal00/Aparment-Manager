import { Injectable, inject } from '@angular/core';
import { Observable, tap, shareReplay, map } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { NotificationService } from '../../core/services/notification.service';
import { 
  ExpenseResponse, 
  ExpenseRequest,
  ExpenseCategorySummary,
  MonthlyExpenseSummary,
  ExpenseTrends,
  ExpenseSearchFilters,
  ExpenseCategory
} from '../models/expense.model';

/**
 * Service for managing expenses
 * Handles expense tracking, categorization, and analytics
 */
@Injectable({
  providedIn: 'root'
})
export class ExpenseService {
  // Inject dependencies
  private readonly api = inject(ApiService);
  private readonly notification = inject(NotificationService);
  
  // API endpoints
  private readonly baseUrl = '/expenses';
  
  // Cache for expense analytics (10-15 minutes TTL)
  private categoryCache = new Map<string, {
    data$: Observable<ExpenseCategorySummary[]>;
    time: number;
  }>();
  private monthlyCache = new Map<string, {
    data$: Observable<MonthlyExpenseSummary>;
    time: number;
  }>();
  private readonly CATEGORY_CACHE_DURATION = 10 * 60 * 1000; // 10 minutes
  private readonly MONTHLY_CACHE_DURATION = 15 * 60 * 1000; // 15 minutes

  /**
   * Get expenses for a building with optional filters
   */
  getExpensesByBuilding(buildingId: number, filters?: ExpenseSearchFilters): Observable<ExpenseResponse[]> {
    let url = `${this.baseUrl}/building/${buildingId}`;
    
    // Build query params from filters
    // Note: The backend only supports startDate and endDate filters
    if (filters) {
      const params = new URLSearchParams();
      if (filters.startDate) params.append('startDate', filters.startDate);
      if (filters.endDate) params.append('endDate', filters.endDate);
      
      const queryString = params.toString();
      if (queryString) {
        url += `?${queryString}`;
      }
    }

    return this.api.get<ExpenseResponse[]>(url).pipe(
      tap(expenses => console.log(`Fetched ${expenses.length} expenses for building ${buildingId}`)),
      // Apply client-side filtering for other criteria
      map(expenses => {
        let filtered = expenses;
        
        if (filters?.category) {
          filtered = filtered.filter(e => e.category === filters.category);
        }
        
        if (filters?.isRecurring !== undefined) {
          filtered = filtered.filter(e => e.isRecurring === filters.isRecurring);
        }
        
        return filtered;
      })
    );
  }

  /**
   * Get expense by ID
   * Note: This endpoint doesn't exist in the backend yet
   * TODO: Implement when backend endpoint is available
   */
  // getExpense(id: number): Observable<ExpenseResponse> {
  //   return this.api.get<ExpenseResponse>(`${this.baseUrl}/${id}`);
  // }

  /**
   * Get expense category summary for a building
   * Uses caching to reduce API calls
   */
  getCategorySummary(buildingId: number, forceRefresh = false): Observable<ExpenseCategorySummary[]> {
    const cacheKey = `category-${buildingId}`;
    const cached = this.categoryCache.get(cacheKey);
    
    // Check cache validity
    if (!forceRefresh && cached && (Date.now() - cached.time) < this.CATEGORY_CACHE_DURATION) {
      return cached.data$;
    }

    // Create new cached observable
    // Using the breakdown endpoint which returns category data
    const summary$ = this.api.get<any>(
      `${this.baseUrl}/building/${buildingId}/breakdown`
    ).pipe(
      map(response => {
        // Transform the response to match ExpenseCategorySummary[]
        if (response.breakdown && Array.isArray(response.breakdown)) {
          return response.breakdown.map((item: any) => ({
            category: item.category,
            categoryName: item.categoryName,
            totalAmount: item.totalAmount,
            count: item.count || 0
          }));
        }
        return [];
      }),
      tap(summary => {
        console.log(`Expense breakdown for building ${buildingId}:`, summary);
        // Log top categories
        const topCategories = summary
          .sort((a: ExpenseCategorySummary, b: ExpenseCategorySummary) => b.totalAmount - a.totalAmount)
          .slice(0, 3);
        console.log('Top 3 expense categories:', topCategories);
      }),
      shareReplay(1)
    );

    // Cache the result
    this.categoryCache.set(cacheKey, {
      data$: summary$,
      time: Date.now()
    });

    return summary$;
  }

  /**
   * Get monthly expense totals
   * Uses longer cache duration as historical data doesn't change
   */
  getMonthlyTotal(buildingId: number, year: number, month: number): Observable<MonthlyExpenseSummary> {
    const cacheKey = `monthly-${buildingId}-${year}-${month}`;
    const cached = this.monthlyCache.get(cacheKey);
    
    // Check cache validity
    if (cached && (Date.now() - cached.time) < this.MONTHLY_CACHE_DURATION) {
      return cached.data$;
    }

    // Create new cached observable
    // Using the monthly-trends endpoint which includes monthly data
    const url = `${this.baseUrl}/building/${buildingId}/monthly-trends?months=1`;
    const monthly$ = this.api.get<any>(url).pipe(
      map(response => {
        // Transform to match MonthlyExpenseSummary
        const currentMonth = `${year}-${month.toString().padStart(2, '0')}`;
        const monthData = response.monthlyTrends?.find((trend: any) => 
          trend.month.startsWith(currentMonth)
        );
        return {
          month: month.toString(),
          year,
          totalAmount: monthData?.total || 0,
          expenseCount: 0, // Not provided by the API
          byCategory: [] // Not provided by this endpoint
        } as MonthlyExpenseSummary;
      }),
      tap(summary => console.log(`Monthly expenses for ${year}-${month}:`, summary)),
      shareReplay(1)
    );

    // Cache the result
    this.monthlyCache.set(cacheKey, {
      data$: monthly$,
      time: Date.now()
    });

    return monthly$;
  }

  /**
   * Get expense trends and analytics
   */
  getExpenseTrends(buildingId: number): Observable<ExpenseTrends> {
    return this.api.get<ExpenseTrends>(`${this.baseUrl}/building/${buildingId}/trend-analysis`).pipe(
      tap(trends => {
        console.log(`Expense trends for building ${buildingId}:`, trends);
        if (trends.changePercentage > 10) {
          console.warn(`Expenses increased by ${trends.changePercentage}% from last month`);
        }
      })
    );
  }

  /**
   * Create a new expense
   */
  createExpense(expense: ExpenseRequest): Observable<ExpenseResponse> {
    return this.api.post<ExpenseResponse>(this.baseUrl, expense).pipe(
      tap(created => {
        this.notification.success(
          `Expense of ${this.formatCurrency(created.amount)} recorded for ${created.categoryDisplayName}`
        );
        
        // Show additional notification if distributed to flats
        if (expense.distributeToFlats) {
          this.notification.info('Expense will be distributed to all flats as monthly dues');
        }
        
        this.invalidateBuildingCache(expense.buildingId);
      })
    );
  }

  /**
   * Update an existing expense
   */
  updateExpense(id: number, expense: ExpenseRequest): Observable<ExpenseResponse> {
    return this.api.put<ExpenseResponse>(`${this.baseUrl}/${id}`, expense).pipe(
      tap(updated => {
        this.notification.success('Expense updated successfully');
        this.invalidateBuildingCache(expense.buildingId);
      })
    );
  }

  /**
   * Delete an expense
   */
  deleteExpense(id: number): Observable<void> {
    return this.api.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => {
        this.notification.success('Expense deleted successfully');
        // Clear all caches as we don't know the building ID
        this.clearCache();
      })
    );
  }

  /**
   * Get recurring expenses for a building
   */
  getRecurringExpenses(buildingId: number): Observable<ExpenseResponse[]> {
    return this.api.get<ExpenseResponse[]>(`${this.baseUrl}/building/${buildingId}/recurring`).pipe(
      tap(expenses => console.log(`Found ${expenses.length} recurring expenses for building ${buildingId}`)),
      map(expenses => expenses.sort((a, b) => a.amount - b.amount))
    );
  }

  /**
   * Get expense category display names
   * Useful for dropdowns and forms
   */
  getExpenseCategories(): { value: ExpenseCategory; label: string }[] {
    return [
      { value: ExpenseCategory.MAINTENANCE, label: 'Maintenance' },
      { value: ExpenseCategory.UTILITIES, label: 'Utilities' },
      { value: ExpenseCategory.CLEANING, label: 'Cleaning' },
      { value: ExpenseCategory.SECURITY, label: 'Security' },
      { value: ExpenseCategory.INSURANCE, label: 'Insurance' },
      { value: ExpenseCategory.TAXES, label: 'Taxes' },
      { value: ExpenseCategory.MANAGEMENT, label: 'Management' },
      { value: ExpenseCategory.REPAIRS, label: 'Repairs' },
      { value: ExpenseCategory.LANDSCAPING, label: 'Landscaping' },
      { value: ExpenseCategory.ELEVATOR, label: 'Elevator' },
      { value: ExpenseCategory.SUPPLIES, label: 'Supplies' },
      { value: ExpenseCategory.LEGAL, label: 'Legal' },
      { value: ExpenseCategory.ACCOUNTING, label: 'Accounting' },
      { value: ExpenseCategory.MARKETING, label: 'Marketing' },
      { value: ExpenseCategory.OTHER, label: 'Other' }
    ];
  }

  /**
   * Format currency for display
   */
  private formatCurrency(amount: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(amount);
  }

  /**
   * Invalidate cache for a specific building
   */
  private invalidateBuildingCache(buildingId: number): void {
    // Clear category cache
    this.categoryCache.delete(`category-${buildingId}`);
    
    // Clear monthly caches for this building
    for (const key of this.monthlyCache.keys()) {
      if (key.startsWith(`monthly-${buildingId}-`)) {
        this.monthlyCache.delete(key);
      }
    }
  }

  /**
   * Clear all caches
   */
  clearCache(): void {
    this.categoryCache.clear();
    this.monthlyCache.clear();
  }
}