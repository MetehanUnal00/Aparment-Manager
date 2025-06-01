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
import { MonthlyDueResponse, DueStatus, DebtorInfo } from '../../../shared/models/monthly-due.model';

// Shared Components
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

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
    CurrencyFormatPipe
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
  overdueDues: MonthlyDueResponse[] = [];
  debtorReport: DebtorInfo[] = [];
  
  // Filters
  showDebtorsOnly = false;
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

    // Load overdue dues
    this.monthlyDueService.getOverdueDues(this.selectedBuildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (overdue) => {
          // Map OverdueSummary to MonthlyDueResponse for display
          this.overdueDues = overdue.map(due => ({
            id: due.dueId,
            flat: {
              id: due.flatId,
              flatNumber: due.flatNumber,
              tenantName: due.tenantName
            },
            dueAmount: due.dueAmount,
            dueDate: due.dueDate,
            status: DueStatus.OVERDUE,
            dueDescription: due.description,
            isOverdue: true,
            createdAt: '',
            updatedAt: ''
          } as MonthlyDueResponse));
          
          this.totalOverdue = overdue.length;
          this.updateStatistics();
        },
        error: (error) => {
          console.error('Error loading overdue dues:', error);
          this.notification.error('Failed to load overdue payments');
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
      this.totalDebt = this.overdueDues.reduce((sum, due) => sum + due.dueAmount, 0);
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
      this.loadMonthlyDues();
    }
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
      this.overdueDues.sort((a, b) => {
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
    console.log('showGenerateMonthlyDues called');
    if (!this.selectedBuildingId) {
      this.notification.warning('Please select a building first');
      return;
    }

    // Set default values
    const today = new Date();
    this.generateMonth = today.getMonth() + 1;
    this.generateYear = today.getFullYear();
    
    // Get default amount from first flat in the building (if available)
    const selectedBuilding = this.buildings.find(b => b.id === this.selectedBuildingId);
    if (selectedBuilding) {
      // You might want to get this from the building's default settings
      this.generateAmount = 1000; // Default amount
    }
    
    this.generateDescription = `Monthly Rent - ${this.getMonthName(this.generateMonth)} ${this.generateYear}`;
    this.showGenerateDialog = true;
    console.log('showGenerateDialog set to:', this.showGenerateDialog);
  }

  /**
   * Generate monthly dues for the building
   */
  generateMonthlyDues(): void {
    if (!this.selectedBuildingId || !this.generateAmount) {
      this.notification.error('Please fill in all required fields');
      return;
    }

    // Calculate due date (1st of the selected month)
    const dueDate = new Date(this.generateYear, this.generateMonth - 1, 1);
    const dueDateStr = dueDate.toISOString().split('T')[0] + 'T00:00:00';

    const request = {
      buildingId: this.selectedBuildingId,
      dueAmount: this.generateAmount,
      dueDate: dueDateStr,
      dueDescription: this.generateDescription
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
    const items = this.showDebtorsOnly ? this.debtorReport : this.overdueDues;
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return items.slice(start, end);
  }

  /**
   * Get total pages
   */
  get totalPages(): number {
    const items = this.showDebtorsOnly ? this.debtorReport : this.overdueDues;
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