import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { forkJoin, Subject, timer, switchMap, takeUntil } from 'rxjs';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { MonthlyDueService } from '../../../shared/services/monthly-due.service';
import { PaymentService } from '../../../shared/services/payment.service';
import { ExpenseService } from '../../../shared/services/expense.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { DebtorInfo } from '../../../shared/models/monthly-due.model';
import { ExpenseCategorySummary } from '../../../shared/models/expense.model';
import { FlatResponse } from '../../../shared/models/flat.model';
import { PaymentSummary } from '../../../shared/models/payment.model';

/**
 * Interface for dashboard summary data
 */
interface DashboardSummary {
  totalBuildings: number;
  totalFlats: number;
  occupiedFlats: number;
  monthlyIncomeTarget: number;
  currentMonthCollection: number;
  collectionRate: number;
  totalDebt: number;
  unpaidDuesCount: number;
  recentPaymentsCount: number;
  currentMonthExpenses: number;
}

/**
 * Dashboard overview component displaying summary statistics
 * Shows key metrics for property management including occupancy,
 * collection rates, and financial performance
 */
@Component({
  selector: 'app-dashboard-overview',
  standalone: true,
  imports: [
    CommonModule, 
    FormsModule, 
    RouterLink, 
    LoadingSpinnerComponent,
    EmptyStateComponent
  ],
  templateUrl: './dashboard-overview.component.html',
  styleUrls: ['./dashboard-overview.component.scss']
})
export class DashboardOverviewComponent implements OnInit, OnDestroy {
  // Inject services using the new inject() function
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly monthlyDueService = inject(MonthlyDueService);
  private readonly paymentService = inject(PaymentService);
  private readonly expenseService = inject(ExpenseService);
  private readonly flatService = inject(FlatService);
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
   * Dashboard summary data
   */
  summary: DashboardSummary = {
    totalBuildings: 0,
    totalFlats: 0,
    occupiedFlats: 0,
    monthlyIncomeTarget: 0,
    currentMonthCollection: 0,
    collectionRate: 0,
    totalDebt: 0,
    unpaidDuesCount: 0,
    recentPaymentsCount: 0,
    currentMonthExpenses: 0
  };

  /**
   * List of buildings for selection
   */
  buildings: ApartmentBuildingResponse[] = [];

  /**
   * Currently selected building ID
   */
  selectedBuildingId: number | null = null;

  /**
   * Flats in the selected building
   */
  flats: FlatResponse[] = [];

  /**
   * Debtor list for the selected building
   */
  debtors: DebtorInfo[] = [];

  /**
   * Payment summary for the selected building
   */
  paymentSummary: PaymentSummary | null = null;

  /**
   * Expense categories summary
   */
  expenseCategories: ExpenseCategorySummary[] = [];

  ngOnInit(): void {
    this.loadDashboardData();
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
        this.loadBuildingSpecificData();
      });
  }

  /**
   * Load all dashboard data
   */
  loadDashboardData(): void {
    this.loading = true;

    // Load buildings first
    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        this.summary.totalBuildings = buildings.length;

        // If we have buildings, select the first one by default
        if (buildings.length > 0) {
          this.selectedBuildingId = buildings[0].id;
          this.loadBuildingSpecificData();
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
   * Load data specific to the selected building
   */
  loadBuildingSpecificData(): void {
    if (!this.selectedBuildingId) return;

    const currentDate = new Date();
    const currentYear = currentDate.getFullYear();
    const currentMonth = currentDate.getMonth() + 1; // JavaScript months are 0-based

    // Create observables for all data we need
    const requests = {
      // Get flats for the building (this will give us occupancy info)
      flats: this.flatService.getFlatsByBuilding(this.selectedBuildingId),
      
      // Get debtor report (includes total debt)
      debtors: this.monthlyDueService.getDebtorReport(this.selectedBuildingId),
      
      // Get payment summary
      paymentSummary: this.paymentService.getPaymentSummary(this.selectedBuildingId),
      
      // Get expense category summary
      expenseCategories: this.expenseService.getCategorySummary(this.selectedBuildingId),
      
      // Get monthly expense total
      monthlyExpenseTotal: this.expenseService.getMonthlyTotal(this.selectedBuildingId, currentYear, currentMonth)
    };

    // Execute all requests in parallel
    forkJoin(requests).subscribe({
      next: (results) => {
        // Update flats data
        this.flats = results.flats;
        this.summary.totalFlats = results.flats.length;
        this.summary.occupiedFlats = results.flats.filter(f => f.tenantName).length;
        
        // Calculate monthly income target (sum of all flat monthly rents)
        this.summary.monthlyIncomeTarget = results.flats.reduce((sum, flat) => sum + (flat.monthlyRent || 0), 0);

        // Update debtor data
        this.debtors = results.debtors;
        this.summary.totalDebt = results.debtors.reduce((sum, debtor) => sum + debtor.totalDebt, 0);
        this.summary.unpaidDuesCount = results.debtors.reduce((count, debtor) => count + debtor.unpaidDuesCount, 0);

        // Update payment summary
        this.paymentSummary = results.paymentSummary;
        this.summary.recentPaymentsCount = results.paymentSummary.paymentCount;
        
        // Calculate collection rate (current month)
        const currentMonthPayments = results.paymentSummary.totalAmount; // This is for the building
        this.summary.currentMonthCollection = currentMonthPayments;
        this.summary.collectionRate = this.summary.monthlyIncomeTarget > 0 
          ? (currentMonthPayments / this.summary.monthlyIncomeTarget) * 100 
          : 0;

        // Update expense categories
        this.expenseCategories = results.expenseCategories;
        
        // Update monthly expense total
        this.summary.currentMonthExpenses = results.monthlyExpenseTotal.totalAmount;

        this.loading = false;
      },
      error: (error) => {
        this.notification.error('Failed to load dashboard data. Please try again.');
        this.loading = false;
        console.error('Error loading dashboard data:', error);
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
      this.loadBuildingSpecificData();
    }
  }

  /**
   * Calculate occupancy rate percentage
   */
  get occupancyRate(): number {
    if (this.summary.totalFlats === 0) return 0;
    return Math.round((this.summary.occupiedFlats / this.summary.totalFlats) * 100);
  }

  /**
   * Calculate collection efficiency
   */
  get collectionEfficiency(): string {
    if (this.summary.monthlyIncomeTarget === 0) return 'N/A';
    const efficiency = (this.summary.currentMonthCollection / this.summary.monthlyIncomeTarget) * 100;
    return efficiency.toFixed(1) + '%';
  }

  /**
   * Get CSS class for collection rate
   */
  getCollectionRateClass(): string {
    if (this.summary.collectionRate >= 90) return 'text-success';
    if (this.summary.collectionRate >= 70) return 'text-warning';
    return 'text-danger';
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
   * Get expense category percentage for display
   */
  getExpenseCategoryPercentage(category: ExpenseCategorySummary): number {
    const total = this.expenseCategories.reduce((sum, cat) => sum + cat.totalAmount, 0);
    return total > 0 ? (category.totalAmount / total) * 100 : 0;
  }

  /**
   * Manual refresh data
   */
  refreshData(): void {
    this.loadBuildingSpecificData();
    this.notification.info('Dashboard data refreshed');
  }

  /**
   * Navigate to add building page
   */
  navigateToAddBuilding = (): void => {
    this.router.navigate(['/buildings', 'new']);
  }

  /**
   * Get display name for expense category
   */
  getCategoryDisplayName(category: string): string {
    const categoryMap: { [key: string]: string } = {
      'MAINTENANCE': 'Maintenance',
      'UTILITIES': 'Utilities',
      'CLEANING': 'Cleaning',
      'SECURITY': 'Security',
      'INSURANCE': 'Insurance',
      'TAXES': 'Taxes',
      'MANAGEMENT': 'Management',
      'REPAIRS': 'Repairs',
      'LANDSCAPING': 'Landscaping',
      'ELEVATOR': 'Elevator',
      'SUPPLIES': 'Supplies',
      'LEGAL': 'Legal',
      'ACCOUNTING': 'Accounting',
      'MARKETING': 'Marketing',
      'OTHER': 'Other'
    };
    return categoryMap[category] || category;
  }
}