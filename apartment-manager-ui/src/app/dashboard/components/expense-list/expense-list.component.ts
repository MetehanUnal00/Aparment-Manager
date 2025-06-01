import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

// Import shared components
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

// Import services
import { ExpenseService } from '../../../shared/services/expense.service';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';

// Import models
import { ExpenseResponse, ExpenseCategory, ExpenseSearchFilters, RecurrenceFrequency } from '../../../shared/models/expense.model';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';

// Import pipes
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-expense-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    ButtonComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    SearchBoxComponent,
    ConfirmDialogComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './expense-list.component.html',
  styleUrls: ['./expense-list.component.scss']
})
export class ExpenseListComponent implements OnInit, OnDestroy {
  // Services injection
  private readonly expenseService = inject(ExpenseService);
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly notification = inject(NotificationService);
  private readonly loading = inject(LoadingService);
  private readonly router = inject(Router);

  // Component state
  expenses: ExpenseResponse[] = [];
  filteredExpenses: ExpenseResponse[] = [];
  buildings: ApartmentBuildingResponse[] = [];
  categories = this.expenseService.getExpenseCategories();
  
  // Filters
  selectedBuildingId: number | null = null;
  selectedCategory: ExpenseCategory | null = null;
  searchTerm = '';
  startDate: string = '';
  endDate: string = '';
  showRecurringOnly = false;
  
  // Sorting
  sortField: keyof ExpenseResponse = 'expenseDate';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // UI state
  isLoading = false;
  showDeleteDialog = false;
  expenseToDelete: ExpenseResponse | null = null;
  
  // Pagination
  currentPage = 1;
  itemsPerPage = 10;
  totalItems = 0;
  
  // Lifecycle management
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadBuildings();
    this.setDefaultDateRange();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load available buildings for filtering
   */
  private loadBuildings(): void {
    this.buildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings: ApartmentBuildingResponse[]) => {
          this.buildings = buildings;
          // If only one building, auto-select it
          if (buildings.length === 1) {
            this.selectedBuildingId = buildings[0].id;
            this.loadExpenses();
          }
        },
        error: (error: any) => {
          console.error('Error loading buildings:', error);
          this.notification.error('Failed to load buildings');
        }
      });
  }

  /**
   * Set default date range to current month
   */
  private setDefaultDateRange(): void {
    const now = new Date();
    const firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
    const lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
    
    this.startDate = this.formatDate(firstDay);
    this.endDate = this.formatDate(lastDay);
  }

  /**
   * Format date to YYYY-MM-DD
   */
  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }

  /**
   * Load expenses based on current filters
   */
  loadExpenses(): void {
    if (!this.selectedBuildingId) {
      this.expenses = [];
      this.filteredExpenses = [];
      return;
    }

    this.isLoading = true;
    
    // Build filters
    const filters: ExpenseSearchFilters = {
      buildingId: this.selectedBuildingId,
      category: this.selectedCategory || undefined,
      startDate: this.startDate || undefined,
      endDate: this.endDate || undefined,
      isRecurring: this.showRecurringOnly ? true : undefined
    };

    this.expenseService.getExpensesByBuilding(this.selectedBuildingId, filters)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.isLoading = false)
      )
      .subscribe({
        next: (expenses) => {
          this.expenses = expenses;
          this.applyFiltersAndSort();
        },
        error: (error) => {
          console.error('Error loading expenses:', error);
          this.notification.error('Failed to load expenses');
          this.expenses = [];
          this.filteredExpenses = [];
        }
      });
  }

  /**
   * Apply local filters and sorting
   */
  private applyFiltersAndSort(): void {
    let filtered = [...this.expenses];

    // Apply search filter
    if (this.searchTerm) {
      const search = this.searchTerm.toLowerCase();
      filtered = filtered.filter(expense => 
        expense.description.toLowerCase().includes(search) ||
        expense.vendorName?.toLowerCase().includes(search) ||
        expense.invoiceNumber?.toLowerCase().includes(search) ||
        expense.categoryDisplayName.toLowerCase().includes(search)
      );
    }

    // Sort
    filtered.sort((a, b) => {
      let aVal = a[this.sortField];
      let bVal = b[this.sortField];
      
      // Handle null/undefined values
      if (aVal === null || aVal === undefined) aVal = '';
      if (bVal === null || bVal === undefined) bVal = '';
      
      // Convert dates to timestamps for comparison
      if (this.sortField === 'expenseDate' || this.sortField === 'createdAt') {
        aVal = new Date(aVal as string).getTime();
        bVal = new Date(bVal as string).getTime();
      }
      
      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });

    this.filteredExpenses = filtered;
    this.totalItems = filtered.length;
  }

  /**
   * Handle building change
   */
  onBuildingChange(): void {
    this.loadExpenses();
  }

  /**
   * Handle filter changes
   */
  onFilterChange(): void {
    this.loadExpenses();
  }

  /**
   * Handle search
   */
  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFiltersAndSort();
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.selectedCategory = null;
    this.searchTerm = '';
    this.showRecurringOnly = false;
    this.setDefaultDateRange();
    this.loadExpenses();
  }

  /**
   * Sort by field
   */
  sortBy(field: keyof ExpenseResponse): void {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = field === 'expenseDate' ? 'desc' : 'asc';
    }
    this.applyFiltersAndSort();
  }

  /**
   * Get sort icon class
   */
  getSortClass(field: keyof ExpenseResponse): string {
    if (this.sortField !== field) return 'bi-arrow-down-up text-muted';
    return this.sortDirection === 'asc' ? 'bi-sort-up' : 'bi-sort-down';
  }

  /**
   * Navigate to create expense
   */
  createExpense(): void {
    const queryParams: any = {};
    if (this.selectedBuildingId) {
      queryParams.buildingId = this.selectedBuildingId;
    }
    this.router.navigate(['/dashboard/expenses/new'], { queryParams });
  }

  /**
   * Navigate to edit expense
   */
  editExpense(expense: ExpenseResponse): void {
    this.router.navigate(['/dashboard/expenses', expense.id, 'edit']);
  }

  /**
   * Confirm delete expense
   */
  confirmDelete(expense: ExpenseResponse, event: Event): void {
    event.stopPropagation();
    this.expenseToDelete = expense;
    this.showDeleteDialog = true;
  }

  /**
   * Delete expense
   */
  deleteExpense(): void {
    if (!this.expenseToDelete) return;

    this.isLoading = true;
    this.expenseService.deleteExpense(this.expenseToDelete.id)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => {
          this.isLoading = false;
          this.showDeleteDialog = false;
          this.expenseToDelete = null;
        })
      )
      .subscribe({
        next: () => {
          this.loadExpenses(); // Reload the list
        },
        error: (error) => {
          console.error('Error deleting expense:', error);
          this.notification.error('Failed to delete expense');
        }
      });
  }

  /**
   * Get recurrence frequency label
   */
  getRecurrenceLabel(frequency: RecurrenceFrequency | undefined): string {
    if (!frequency) return '';
    
    const labels: Record<RecurrenceFrequency, string> = {
      [RecurrenceFrequency.WEEKLY]: 'Weekly',
      [RecurrenceFrequency.MONTHLY]: 'Monthly',
      [RecurrenceFrequency.QUARTERLY]: 'Quarterly',
      [RecurrenceFrequency.SEMI_ANNUAL]: 'Semi-Annual',
      [RecurrenceFrequency.ANNUAL]: 'Annual'
    };
    
    return labels[frequency] || '';
  }

  /**
   * Get category icon class
   */
  getCategoryIcon(category: ExpenseCategory): string {
    const icons: Partial<Record<ExpenseCategory, string>> = {
      [ExpenseCategory.MAINTENANCE]: 'bi-tools',
      [ExpenseCategory.UTILITIES]: 'bi-lightning-charge',
      [ExpenseCategory.CLEANING]: 'bi-stars',
      [ExpenseCategory.SECURITY]: 'bi-shield-lock',
      [ExpenseCategory.INSURANCE]: 'bi-shield-check',
      [ExpenseCategory.TAXES]: 'bi-receipt',
      [ExpenseCategory.MANAGEMENT]: 'bi-briefcase',
      [ExpenseCategory.REPAIRS]: 'bi-wrench',
      [ExpenseCategory.LANDSCAPING]: 'bi-tree',
      [ExpenseCategory.ELEVATOR]: 'bi-arrow-up-square',
      [ExpenseCategory.SUPPLIES]: 'bi-box',
      [ExpenseCategory.LEGAL]: 'bi-scale',
      [ExpenseCategory.ACCOUNTING]: 'bi-calculator',
      [ExpenseCategory.MARKETING]: 'bi-megaphone',
      [ExpenseCategory.OTHER]: 'bi-three-dots'
    };
    
    return icons[category] || 'bi-three-dots';
  }

  /**
   * Get paginated expenses
   */
  get paginatedExpenses(): ExpenseResponse[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    const end = start + this.itemsPerPage;
    return this.filteredExpenses.slice(start, end);
  }

  /**
   * Get total pages
   */
  get totalPages(): number {
    return Math.ceil(this.totalItems / this.itemsPerPage);
  }

  /**
   * Change page
   */
  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  /**
   * Calculate total amount for displayed expenses
   */
  get totalAmount(): number {
    return this.filteredExpenses.reduce((sum, expense) => sum + expense.amount, 0);
  }
}