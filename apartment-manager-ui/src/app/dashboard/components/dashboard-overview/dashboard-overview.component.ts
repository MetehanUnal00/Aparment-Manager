import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApartmentBuildingService, BuildingStatistics } from '../../../shared/services/apartment-building.service';
import { MonthlyDueService, CollectionRate } from '../../../shared/services/monthly-due.service';
import { PaymentService, PaymentStatistics } from '../../../shared/services/payment.service';
import { ExpenseService, CategorySummary } from '../../../shared/services/expense.service';

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
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './dashboard-overview.component.html',
  styleUrls: ['./dashboard-overview.component.scss']
})
export class DashboardOverviewComponent implements OnInit {
  /**
   * Loading state indicator
   */
  loading = true;

  /**
   * Error message if data loading fails
   */
  errorMessage = '';

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
  buildings: any[] = [];

  /**
   * Currently selected building ID
   */
  selectedBuildingId: number | null = null;

  /**
   * Building-specific statistics
   */
  buildingStats: BuildingStatistics | null = null;

  /**
   * Collection rate data for the selected building
   */
  collectionRate: CollectionRate | null = null;

  /**
   * Payment statistics for the selected building
   */
  paymentStats: PaymentStatistics | null = null;

  /**
   * Expense categories summary
   */
  expenseCategories: CategorySummary[] = [];

  constructor(
    private buildingService: ApartmentBuildingService,
    private monthlyDueService: MonthlyDueService,
    private paymentService: PaymentService,
    private expenseService: ExpenseService
  ) { }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  /**
   * Load all dashboard data
   */
  loadDashboardData(): void {
    this.loading = true;
    this.errorMessage = '';

    // Load buildings first
    this.buildingService.getAllBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        this.summary.totalBuildings = buildings.length;

        // If we have buildings, select the first one by default
        if (buildings.length > 0) {
          this.selectedBuildingId = buildings[0].id!;
          this.loadBuildingSpecificData();
        } else {
          this.loading = false;
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
   * Load data specific to the selected building
   */
  loadBuildingSpecificData(): void {
    if (!this.selectedBuildingId) return;

    const currentDate = new Date();
    const startOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth(), 1);
    const endOfMonth = new Date(currentDate.getFullYear(), currentDate.getMonth() + 1, 0);

    // Create observables for all data we need
    const requests = {
      buildingStats: this.buildingService.getBuildingStatistics(this.selectedBuildingId),
      collectionRate: this.monthlyDueService.getCollectionRate(
        this.selectedBuildingId,
        currentDate.getMonth() + 1,
        currentDate.getFullYear()
      ),
      paymentStats: this.paymentService.getPaymentStatistics(
        this.selectedBuildingId,
        startOfMonth.toISOString().split('T')[0],
        endOfMonth.toISOString().split('T')[0]
      ),
      unpaidDuesCount: this.monthlyDueService.getUnpaidDuesCount(this.selectedBuildingId),
      expenseCategories: this.expenseService.getCategorySummary(
        this.selectedBuildingId,
        startOfMonth.toISOString().split('T')[0],
        endOfMonth.toISOString().split('T')[0]
      )
    };

    // Execute all requests in parallel
    forkJoin(requests).subscribe({
      next: (results) => {
        // Update building statistics
        this.buildingStats = results.buildingStats;
        this.summary.totalFlats = results.buildingStats.totalFlats;
        this.summary.occupiedFlats = results.buildingStats.occupiedFlats;
        this.summary.monthlyIncomeTarget = results.buildingStats.monthlyIncomeTarget;
        this.summary.totalDebt = results.buildingStats.totalDebt;

        // Update collection rate
        this.collectionRate = results.collectionRate;
        this.summary.collectionRate = results.collectionRate.collectionRate;
        this.summary.currentMonthCollection = results.collectionRate.totalPaidAmount;

        // Update payment statistics
        this.paymentStats = results.paymentStats;
        this.summary.recentPaymentsCount = results.paymentStats.totalCount;

        // Update unpaid dues count
        this.summary.unpaidDuesCount = results.unpaidDuesCount;

        // Update expense categories
        this.expenseCategories = results.expenseCategories;
        this.summary.currentMonthExpenses = results.expenseCategories.reduce(
          (total, cat) => total + cat.totalAmount, 
          0
        );

        this.loading = false;
      },
      error: (error) => {
        this.errorMessage = 'Failed to load dashboard data. Please try again.';
        this.loading = false;
        console.error('Error loading dashboard data:', error);
      }
    });
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(buildingId: number): void {
    this.selectedBuildingId = buildingId;
    this.loadBuildingSpecificData();
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
}