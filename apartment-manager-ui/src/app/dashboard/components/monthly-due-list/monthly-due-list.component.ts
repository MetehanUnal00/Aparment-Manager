import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// Services
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { MonthlyDueService } from '../../../shared/services/monthly-due.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';

// Models
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { MonthlyDueResponse, DueStatus, DebtorInfo, MonthlyDueRequest } from '../../../shared/models/monthly-due.model';

// Shared Components
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

// Directives
import { ResponsiveTableDirective } from '../../../shared/directives/responsive-table.directive';

// Pipes
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

/**
 * Component for managing monthly dues
 * Displays overdue payments and allows generating new monthly dues
 */
@Component({
  selector: 'app-monthly-due-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    ButtonComponent,
    ConfirmDialogComponent,
    CurrencyFormatPipe,
    ResponsiveTableDirective
  ],
  templateUrl: './monthly-due-list.component.html',
  styleUrls: ['./monthly-due-list.component.scss']
})
export class MonthlyDueListComponent implements OnInit, OnDestroy {
  // Inject services
  private router = inject(Router);
  private buildingService = inject(ApartmentBuildingService);
  private monthlyDueService = inject(MonthlyDueService);
  private notification = inject(NotificationService);
  loading = inject(LoadingService);

  // Component state
  buildings: ApartmentBuildingResponse[] = [];
  selectedBuildingId: number | null = null;
  allDues: MonthlyDueResponse[] = [];
  overdueDues: MonthlyDueResponse[] = [];
  debtorReport: DebtorInfo[] = [];
  
  // Filters
  showDebtorsOnly = false;
  showOverdueOnly = false;
  sortBy: 'dueDate' | 'amount' | 'flat' = 'dueDate';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Statistics
  totalOverdue = 0;
  totalDebt = 0;
  totalDebtors = 0;
  
  // UI state
  showGenerateDialog = false;
  generateMonth = new Date().getMonth() + 1;
  generateYear = new Date().getFullYear();
  generateAmount = 0;
  generateDescription = '';
  generateMode: 'uniform' | 'rentBased' = 'uniform';
  fallbackAmount: number | null = null;
  
  // Pagination
  currentPage = 1;
  pageSize = 20;
  
  // Component lifecycle
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadBuildings();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all buildings for the user
   */
  private loadBuildings(): void {
    this.buildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings: ApartmentBuildingResponse[]) => {
          this.buildings = buildings;
          
          // Auto-select first building if only one exists
          if (buildings.length === 1) {
            this.selectedBuildingId = buildings[0].id;
            this.loadMonthlyDues();
          }
        },
        error: (error: any) => {
          console.error('Error loading buildings:', error);
          this.notification.error('Failed to load buildings');
        }
      });
  }

  /**
   * Load monthly dues for selected building
   */
  loadMonthlyDues(): void {
    if (!this.selectedBuildingId) return;

    // Load all dues
    this.monthlyDueService.getAllDuesForBuilding(this.selectedBuildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dues) => {
          this.allDues = dues;
          
          // Filter overdue dues
          const today = new Date();
          today.setHours(0, 0, 0, 0);
          
          this.overdueDues = dues.filter(due => {
            const dueDate = new Date(due.dueDate);
            return due.status !== DueStatus.PAID && 
                   due.status !== DueStatus.CANCELLED && 
                   dueDate < today;
          });
          
          this.totalOverdue = this.overdueDues.length;
          this.updateStatistics();
        },
        error: (error) => {
          console.error('Error loading monthly dues:', error);
          this.notification.error('Failed to load monthly dues');
        }
      });

    // Load debtor report if showing debtors
    if (this.showDebtorsOnly) {
      this.loadDebtorReport();
    }
  }

  /**
   * Load debtor report for the building
   */
  loadDebtorReport(): void {
    if (!this.selectedBuildingId) return;

    this.monthlyDueService.getDebtorReport(this.selectedBuildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (debtors) => {
          this.debtorReport = debtors;
          this.totalDebtors = debtors.length;
          this.totalDebt = debtors.reduce((sum, d) => sum + d.totalDebt, 0);
        },
        error: (error) => {
          console.error('Error loading debtor report:', error);
          this.notification.error('Failed to load debtor report');
        }
      });
  }

  /**
   * Update statistics from current data
   */
  private updateStatistics(): void {
    if (!this.showDebtorsOnly) {
      const duesToCalculate = this.showOverdueOnly ? this.overdueDues : this.allDues;
      this.totalDebt = duesToCalculate
        .filter(due => due.status !== DueStatus.PAID && due.status !== DueStatus.CANCELLED)
        .reduce((sum, due) => sum + due.dueAmount, 0);
    }
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(): void {
    this.currentPage = 1;
    this.loadMonthlyDues();
  }

  /**
   * Toggle between overdue and debtors view
   */
  toggleView(): void {
    this.showDebtorsOnly = !this.showDebtorsOnly;
    this.currentPage = 1;
    
    if (this.showDebtorsOnly) {
      this.loadDebtorReport();
    } else {
      this.updateStatistics();
    }
  }

  /**
   * Toggle between all dues and overdue only
   */
  toggleOverdueFilter(): void {
    this.showOverdueOnly = !this.showOverdueOnly;
    this.currentPage = 1;
    this.updateStatistics();
  }

  /**
   * Sort the displayed items
   */
  sort(column: 'dueDate' | 'amount' | 'flat'): void {
    if (this.sortBy === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDirection = 'asc';
    }

    // Apply sorting
    if (!this.showDebtorsOnly) {
      const duesToSort = this.showOverdueOnly ? this.overdueDues : this.allDues;
      duesToSort.sort((a, b) => {
        let comparison = 0;
        
        switch (column) {
          case 'dueDate':
            comparison = new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
            break;
          case 'amount':
            comparison = a.dueAmount - b.dueAmount;
            break;
          case 'flat':
            comparison = a.flat.flatNumber.localeCompare(b.flat.flatNumber);
            break;
        }
        
        return this.sortDirection === 'asc' ? comparison : -comparison;
      });
    } else {
      this.debtorReport.sort((a, b) => {
        let comparison = 0;
        
        switch (column) {
          case 'dueDate':
            comparison = new Date(a.oldestUnpaidDueDate).getTime() - new Date(b.oldestUnpaidDueDate).getTime();
            break;
          case 'amount':
            comparison = a.totalDebt - b.totalDebt;
            break;
          case 'flat':
            comparison = a.flatNumber.localeCompare(b.flatNumber);
            break;
        }
        
        return this.sortDirection === 'asc' ? comparison : -comparison;
      });
    }
  }

  /**
   * Show generate monthly dues dialog
   */
  showGenerateMonthlyDues(): void {
    if (!this.selectedBuildingId) {
      this.notification.warning('Please select a building first');
      return;
    }

    // Set default values
    const today = new Date();
    // For June (month 6), getMonth() returns 5, so we add 1
    const currentMonth = today.getMonth() + 1;
    const currentYear = today.getFullYear();
    
    // Default to next month if we're past the 1st of current month
    if (today.getDate() > 1) {
      if (currentMonth === 12) {
        this.generateMonth = 1;
        this.generateYear = currentYear + 1;
      } else {
        this.generateMonth = currentMonth + 1;
        this.generateYear = currentYear;
      }
    } else {
      this.generateMonth = currentMonth;
      this.generateYear = currentYear;
    }
    
    console.log('Default month/year set to:', this.generateMonth, this.generateYear);
    
    // Reset generation mode
    this.generateMode = 'uniform';
    this.fallbackAmount = null;
    
    // Get default amount from first flat in the building (if available)
    const selectedBuilding = this.buildings.find(b => b.id === this.selectedBuildingId);
    if (selectedBuilding) {
      // Set a reasonable default amount
      this.generateAmount = 1000; // Default amount
    }
    
    this.generateDescription = `Monthly Rent - ${this.getMonthName(this.generateMonth)} ${this.generateYear}`;
    this.showGenerateDialog = true;
  }

  /**
   * Generate monthly dues for the building
   */
  generateMonthlyDues(): void {
    // Validation based on mode
    if (this.generateMode === 'uniform') {
      if (!this.selectedBuildingId || !this.generateAmount) {
        this.notification.error('Please fill in all required fields');
        return;
      }
    } else {
      // Rent-based mode - need at least one fallback
      if (!this.selectedBuildingId || (!this.fallbackAmount && !this.generateAmount)) {
        this.notification.error('Please provide at least one fallback amount for flats without rent');
        return;
      }
    }

    // Ensure month and year are numbers
    const month = Number(this.generateMonth);
    const year = Number(this.generateYear);
    
    console.log('Generating dues for:', { month, year, generateMonth: this.generateMonth, generateYear: this.generateYear });

    // Calculate due date (1st of the selected month)
    const dueDate = new Date(year, month - 1, 1);
    
    // Ensure the date is not in the past
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (dueDate < today) {
      this.notification.error('Due date cannot be in the past. Please select a future month.');
      return;
    }
    
    const dueDateStr = dueDate.toISOString().split('T')[0] + 'T00:00:00';
    console.log('Due date string:', dueDateStr);

    const request: MonthlyDueRequest = {
      buildingId: this.selectedBuildingId,
      dueAmount: this.generateAmount,
      dueDate: dueDateStr,
      dueDescription: this.generateDescription,
      useFlatsMonthlyRent: this.generateMode === 'rentBased',
      fallbackAmount: this.generateMode === 'rentBased' && this.fallbackAmount !== null ? this.fallbackAmount : undefined
    };

    this.monthlyDueService.generateMonthlyDues(request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (dues) => {
          this.notification.success(`Generated ${dues.length} monthly dues successfully`);
          this.showGenerateDialog = false;
          this.loadMonthlyDues();
        },
        error: (error) => {
          console.error('Error generating monthly dues:', error);
          this.notification.error('Failed to generate monthly dues');
        }
      });
  }

  /**
   * Handle generation mode change
   */
  onGenerationModeChange(): void {
    // Preserve the existing generateAmount value
    // Clear fallback when switching to uniform mode
    if (this.generateMode === 'uniform') {
      this.fallbackAmount = null;
    }
  }

  /**
   * Cancel a monthly due
   */
  cancelDue(due: MonthlyDueResponse): void {
    if (confirm(`Are you sure you want to cancel the due for ${due.flat.flatNumber}?`)) {
      this.monthlyDueService.cancelMonthlyDue(due.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.notification.success('Monthly due cancelled successfully');
            this.loadMonthlyDues();
          },
          error: (error) => {
            console.error('Error cancelling due:', error);
            this.notification.error('Failed to cancel monthly due');
          }
        });
    }
  }

  /**
   * Navigate to create individual monthly due
   */
  createIndividualDue(): void {
    this.router.navigate(['/dashboard/monthly-dues/new'], {
      queryParams: { buildingId: this.selectedBuildingId }
    });
  }

  /**
   * Navigate to edit monthly due
   */
  editDue(dueId: number): void {
    this.router.navigate(['/dashboard/monthly-dues', dueId, 'edit']);
  }

  /**
   * Get month name from number
   */
  private getMonthName(month: number): string {
    const months = [
      'January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'
    ];
    return months[month - 1] || '';
  }

  /**
   * Get paginated items
   */
  get paginatedItems(): any[] {
    const items = this.showDebtorsOnly 
      ? this.debtorReport 
      : (this.showOverdueOnly ? this.overdueDues : this.allDues);
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return items.slice(start, end);
  }

  /**
   * Get total pages
   */
  get totalPages(): number {
    const items = this.showDebtorsOnly 
      ? this.debtorReport 
      : (this.showOverdueOnly ? this.overdueDues : this.allDues);
    return Math.ceil(items.length / this.pageSize);
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
   * Calculate days overdue
   */
  getDaysOverdue(dueDate: string): number {
    const due = new Date(dueDate);
    const today = new Date();
    const diffTime = today.getTime() - due.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays > 0 ? diffDays : 0;
  }
}