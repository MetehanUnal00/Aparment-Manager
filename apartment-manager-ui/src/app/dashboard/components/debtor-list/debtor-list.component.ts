import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { MonthlyDueService, DebtorInfo } from '../../../shared/services/monthly-due.service';

/**
 * Component displaying list of debtors with overdue payments
 * Shows tenant information, debt amounts, and allows actions like
 * sending notifications or recording payments
 */
@Component({
  selector: 'app-debtor-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './debtor-list.component.html',
  styleUrls: ['./debtor-list.component.scss']
})
export class DebtorListComponent implements OnInit {
  /**
   * Loading state indicator
   */
  loading = true;

  /**
   * Error message if data loading fails
   */
  errorMessage = '';

  /**
   * List of buildings for selection
   */
  buildings: any[] = [];

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
  sortColumn: keyof DebtorInfo = 'daysOverdue';

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

  constructor(
    private buildingService: ApartmentBuildingService,
    private monthlyDueService: MonthlyDueService
  ) { }

  ngOnInit(): void {
    this.loadBuildings();
  }

  /**
   * Load all buildings
   */
  private loadBuildings(): void {
    this.loading = true;
    this.errorMessage = '';

    this.buildingService.getAllBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        
        // Select first building by default
        if (buildings.length > 0) {
          this.selectedBuildingId = buildings[0].id!;
          this.loadDebtors();
        } else {
          this.loading = false;
          this.errorMessage = 'No buildings found. Please add a building first.';
        }
      },
      error: (error) => {
        this.errorMessage = 'Failed to load buildings. Please try again.';
        this.loading = false;
        console.error('Error loading buildings:', error);
      }
    });
  }

  /**
   * Load debtors for the selected building
   */
  private loadDebtors(): void {
    if (!this.selectedBuildingId) return;

    this.loading = true;
    this.errorMessage = '';

    this.monthlyDueService.getDebtorsList(this.selectedBuildingId).subscribe({
      next: (debtors) => {
        this.debtors = debtors;
        this.filteredDebtors = debtors;
        this.calculateTotalDebt();
        this.sortDebtors();
        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load debtors. Please try again.';
        this.loading = false;
        console.error('Error loading debtors:', error);
      }
    });
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(buildingId: number): void {
    this.selectedBuildingId = buildingId;
    this.selectedDebtors.clear();
    this.loadDebtors();
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
    if (!this.selectedBuildingId) return;

    this.sendingNotifications = true;
    
    this.monthlyDueService.sendOverdueNotifications(this.selectedBuildingId).subscribe({
      next: (result) => {
        alert(`Notifications sent! Success: ${result.sentCount}, Failed: ${result.failedCount}`);
        this.sendingNotifications = false;
        this.selectedDebtors.clear();
      },
      error: (error) => {
        alert('Failed to send notifications. Please try again.');
        this.sendingNotifications = false;
        console.error('Error sending notifications:', error);
      }
    });
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
}