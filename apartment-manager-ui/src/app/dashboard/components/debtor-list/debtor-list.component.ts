import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { Subject, timer, switchMap, takeUntil } from 'rxjs';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { MonthlyDueService } from '../../../shared/services/monthly-due.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { DebtorInfo } from '../../../shared/models/monthly-due.model';

/**
 * Component displaying list of debtors with overdue payments
 * Shows tenant information, debt amounts, and allows actions like
 * sending notifications or recording payments
 */
@Component({
  selector: 'app-debtor-list',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    SearchBoxComponent
  ],
  templateUrl: './debtor-list.component.html',
  styleUrls: ['./debtor-list.component.scss']
})
export class DebtorListComponent implements OnInit, OnDestroy {
  // Inject services using the new inject() function
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly monthlyDueService = inject(MonthlyDueService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);

  /**
   * Subject for component destruction
   */
  private readonly destroy$ = new Subject<void>();

  /**
   * Loading state indicator
   */
  loading = true;

  /**
   * Auto-refresh enabled state
   */
  autoRefreshEnabled = false;

  /**
   * List of buildings for selection
   */
  buildings: ApartmentBuildingResponse[] = [];

  /**
   * Currently selected building ID
   */
  selectedBuildingId: number | null = null;

  /**
   * List of debtors with their information
   */
  debtors: DebtorInfo[] = [];

  /**
   * Filtered list of debtors based on search
   */
  filteredDebtors: DebtorInfo[] = [];

  /**
   * Search term for filtering debtors
   */
  searchTerm = '';

  /**
   * Sort column name
   */
  sortColumn: keyof DebtorInfo = 'overdueDays';

  /**
   * Sort direction
   */
  sortDirection: 'asc' | 'desc' = 'desc';

  /**
   * Total debt amount across all debtors
   */
  totalDebt = 0;

  /**
   * Selected debtors for bulk actions
   */
  selectedDebtors: Set<number> = new Set();

  /**
   * Flag indicating if notification is being sent
   */
  sendingNotifications = false;

  ngOnInit(): void {
    this.loadBuildings();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggle auto-refresh functionality
   */
  toggleAutoRefresh(): void {
    this.autoRefreshEnabled = !this.autoRefreshEnabled;
    
    if (this.autoRefreshEnabled) {
      this.notification.info('Auto-refresh enabled (every 30 seconds)');
      this.startAutoRefresh();
    } else {
      this.notification.info('Auto-refresh disabled');
    }
  }

  /**
   * Start auto-refresh timer
   */
  private startAutoRefresh(): void {
    // Refresh every 30 seconds when auto-refresh is enabled
    timer(30000, 30000)
      .pipe(
        takeUntil(this.destroy$),
        switchMap(() => this.autoRefreshEnabled ? [true] : [])
      )
      .subscribe(() => {
        this.loadDebtors(true);
      });
  }

  /**
   * Load all buildings
   */
  private loadBuildings(): void {
    this.loading = true;

    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        
        // Select first building by default
        if (buildings.length > 0) {
          this.selectedBuildingId = buildings[0].id;
          this.loadDebtors();
        } else {
          this.loading = false;
        }
      },
      error: (error) => {
        this.notification.error('Failed to load buildings. Please try again.');
        this.loading = false;
        console.error('Error loading buildings:', error);
      }
    });
  }

  /**
   * Load debtors for the selected building
   */
  private loadDebtors(forceRefresh = false): void {
    if (!this.selectedBuildingId) return;

    if (!forceRefresh) {
      this.loading = true;
    }

    this.monthlyDueService.getDebtorReport(this.selectedBuildingId, forceRefresh).subscribe({
      next: (debtors) => {
        this.debtors = debtors;
        this.filteredDebtors = debtors;
        this.calculateTotalDebt();
        this.sortDebtors();
        this.loading = false;
      },
      error: (error) => {
        this.notification.error('Failed to load debtors. Please try again.');
        this.loading = false;
        console.error('Error loading debtors:', error);
      }
    });
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const buildingId = parseInt(target.value, 10);
    if (!isNaN(buildingId)) {
      this.selectedBuildingId = buildingId;
      this.selectedDebtors.clear();
      this.loadDebtors();
    }
  }

  /**
   * Filter debtors based on search term
   */
  filterDebtors(): void {
    if (!this.searchTerm) {
      this.filteredDebtors = this.debtors;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredDebtors = this.debtors.filter(debtor =>
        debtor.flatNumber.toLowerCase().includes(term) ||
        (debtor.tenantName && debtor.tenantName.toLowerCase().includes(term))
      );
    }
    this.calculateTotalDebt();
  }

  /**
   * Sort debtors by specified column
   */
  sort(column: keyof DebtorInfo): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.sortDebtors();
  }

  /**
   * Perform the actual sorting
   */
  private sortDebtors(): void {
    this.filteredDebtors.sort((a, b) => {
      let aValue = a[this.sortColumn];
      let bValue = b[this.sortColumn];

      // Handle null/undefined values
      if (aValue == null) aValue = '';
      if (bValue == null) bValue = '';

      // Convert to numbers for numeric columns
      if (typeof aValue === 'number' && typeof bValue === 'number') {
        return this.sortDirection === 'asc' ? aValue - bValue : bValue - aValue;
      }

      // String comparison for other columns
      const comparison = String(aValue).localeCompare(String(bValue));
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  /**
   * Calculate total debt amount
   */
  private calculateTotalDebt(): void {
    this.totalDebt = this.filteredDebtors.reduce((sum, debtor) => sum + debtor.totalDebt, 0);
  }

  /**
   * Toggle selection of a debtor
   */
  toggleSelection(flatId: number): void {
    if (this.selectedDebtors.has(flatId)) {
      this.selectedDebtors.delete(flatId);
    } else {
      this.selectedDebtors.add(flatId);
    }
  }

  /**
   * Toggle selection of all visible debtors
   */
  toggleAllSelection(): void {
    if (this.selectedDebtors.size === this.filteredDebtors.length) {
      this.selectedDebtors.clear();
    } else {
      this.filteredDebtors.forEach(debtor => this.selectedDebtors.add(debtor.flatId));
    }
  }

  /**
   * Handle search term change
   */
  onSearchChange(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.filterDebtors();
  }

  /**
   * Check if a debtor is selected
   */
  isSelected(flatId: number): boolean {
    return this.selectedDebtors.has(flatId);
  }

  /**
   * Check if all visible debtors are selected
   */
  get allSelected(): boolean {
    return this.filteredDebtors.length > 0 && 
           this.selectedDebtors.size === this.filteredDebtors.length;
  }

  /**
   * Check if some but not all debtors are selected
   */
  get someSelected(): boolean {
    return this.selectedDebtors.size > 0 && 
           this.selectedDebtors.size < this.filteredDebtors.length;
  }

  /**
   * Send notifications to selected debtors
   */
  sendNotifications(): void {
    if (!this.selectedBuildingId || this.selectedDebtors.size === 0) {
      this.notification.warning('Please select at least one debtor to send notifications');
      return;
    }

    this.sendingNotifications = true;
    
    // TODO: Implement when backend endpoint is available
    // For now, we'll simulate the notification sending
    setTimeout(() => {
      this.notification.info('Reminder notification feature is not yet implemented in the backend');
      this.sendingNotifications = false;
      this.selectedDebtors.clear();
    }, 1000);
    
    // Original code commented out until backend endpoint is available:
    // this.monthlyDueService.sendReminders(this.selectedBuildingId).subscribe({
    //   next: (result: any) => {
    //     this.notification.success(`Sent ${result.sent} reminder emails successfully`);
    //     if (result.failed > 0) {
    //       this.notification.warning(`Failed to send ${result.failed} emails`);
    //     }
    //     this.sendingNotifications = false;
    //     this.selectedDebtors.clear();
    //     // Refresh the debtor list
    //     this.loadDebtors(true);
    //   },
    //   error: (error: any) => {
    //     this.notification.error('Failed to send notifications. Please try again.');
    //     this.sendingNotifications = false;
    //     console.error('Error sending notifications:', error);
    //   }
    // });
  }

  /**
   * Format currency values
   */
  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD'
    }).format(value);
  }

  /**
   * Format date values
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }

  /**
   * Get CSS class based on days overdue
   */
  getOverdueClass(daysOverdue: number): string {
    if (daysOverdue >= 60) return 'text-danger fw-bold';
    if (daysOverdue >= 30) return 'text-warning';
    return '';
  }

  /**
   * Get badge class based on overdue count
   */
  getBadgeClass(overdueCount: number): string {
    if (overdueCount >= 3) return 'bg-danger';
    if (overdueCount >= 2) return 'bg-warning';
    return 'bg-secondary';
  }

  /**
   * Manual refresh data
   */
  refreshData(): void {
    this.loadDebtors(true);
    this.notification.info('Debtor list refreshed');
  }

  /**
   * Navigate to add building page
   */
  navigateToAddBuilding = (): void => {
    this.router.navigate(['/buildings', 'new']);
  }
}