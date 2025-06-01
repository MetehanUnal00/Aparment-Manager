import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil, finalize } from 'rxjs';

// Import shared components
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';

// Import services
import { ExpenseService } from '../../../shared/services/expense.service';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';

// Import models
import { ExpenseRequest, ExpenseResponse, ExpenseCategory, RecurrenceFrequency } from '../../../shared/models/expense.model';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';

// Import directives
import { AutofocusDirective } from '../../../shared/directives/autofocus.directive';

@Component({
  selector: 'app-expense-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    LoadingSpinnerComponent,
    FormErrorsComponent,
    AutofocusDirective
  ],
  templateUrl: './expense-form.component.html',
  styleUrls: ['./expense-form.component.scss']
})
export class ExpenseFormComponent implements OnInit, OnDestroy {
  // Services injection
  private readonly expenseService = inject(ExpenseService);
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly notification = inject(NotificationService);
  private readonly loading = inject(LoadingService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  // Form data
  expense: ExpenseRequest = {
    buildingId: 0,
    category: ExpenseCategory.OTHER,
    amount: 0,
    expenseDate: this.getToday(),
    description: '',
    vendorName: '',
    invoiceNumber: '',
    isRecurring: false,
    recurrenceFrequency: undefined,
    distributeToFlats: false
  };

  // Edit mode
  isEditMode = false;
  expenseId?: number;
  originalExpense?: ExpenseResponse;

  // Data
  buildings: ApartmentBuildingResponse[] = [];
  categories = this.expenseService.getExpenseCategories();
  recurrenceOptions = [
    { value: RecurrenceFrequency.WEEKLY, label: 'Weekly' },
    { value: RecurrenceFrequency.MONTHLY, label: 'Monthly' },
    { value: RecurrenceFrequency.QUARTERLY, label: 'Quarterly' },
    { value: RecurrenceFrequency.SEMI_ANNUAL, label: 'Semi-Annual' },
    { value: RecurrenceFrequency.ANNUAL, label: 'Annual' }
  ];

  // UI state
  isLoading = false;
  isSubmitting = false;
  maxDate = this.getToday();

  // Lifecycle management
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    this.loadBuildings();
    this.checkEditMode();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Get today's date in YYYY-MM-DD format
   */
  private getToday(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Load available buildings
   */
  private loadBuildings(): void {
    this.buildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings: ApartmentBuildingResponse[]) => {
          this.buildings = buildings;
          
          // Pre-select building from query params if provided
          const buildingIdParam = this.route.snapshot.queryParams['buildingId'];
          if (buildingIdParam && !this.isEditMode) {
            const buildingId = parseInt(buildingIdParam, 10);
            if (this.buildings.find(b => b.id === buildingId)) {
              this.expense.buildingId = buildingId;
            }
          }
          
          // Auto-select if only one building
          if (buildings.length === 1 && !this.expense.buildingId) {
            this.expense.buildingId = buildings[0].id;
          }
        },
        error: (error: any) => {
          console.error('Error loading buildings:', error);
          this.notification.error('Failed to load buildings');
        }
      });
  }

  /**
   * Check if in edit mode and load expense
   */
  private checkEditMode(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.isEditMode = true;
      this.expenseId = parseInt(id, 10);
      // Note: Since there's no get expense by ID endpoint, we'd need to load from list
      // For now, we'll show an error
      this.notification.error('Edit functionality requires a backend endpoint to get expense by ID');
      this.router.navigate(['/dashboard/expenses']);
    }
  }

  /**
   * Handle form submission
   */
  onSubmit(form: NgForm): void {
    if (form.invalid) {
      Object.keys(form.controls).forEach(key => {
        form.controls[key].markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;

    // Convert date to LocalDateTime format if needed
    const expenseData: ExpenseRequest = {
      ...this.expense,
      expenseDate: this.expense.expenseDate.includes('T') 
        ? this.expense.expenseDate 
        : this.expense.expenseDate + 'T00:00:00'
    };

    // Clear recurrence frequency if not recurring
    if (!expenseData.isRecurring) {
      expenseData.recurrenceFrequency = undefined;
    }

    const operation = this.isEditMode && this.expenseId
      ? this.expenseService.updateExpense(this.expenseId, expenseData)
      : this.expenseService.createExpense(expenseData);

    operation.pipe(
      takeUntil(this.destroy$),
      finalize(() => this.isSubmitting = false)
    ).subscribe({
      next: () => {
        this.router.navigate(['/dashboard/expenses']);
      },
      error: (error) => {
        console.error('Error saving expense:', error);
        this.notification.error(
          this.isEditMode ? 'Failed to update expense' : 'Failed to create expense'
        );
      }
    });
  }

  /**
   * Cancel and go back
   */
  cancel(): void {
    this.router.navigate(['/dashboard/expenses']);
  }

  /**
   * Handle recurring checkbox change
   */
  onRecurringChange(): void {
    if (!this.expense.isRecurring) {
      this.expense.recurrenceFrequency = undefined;
    } else if (!this.expense.recurrenceFrequency) {
      // Default to monthly if enabling recurring
      this.expense.recurrenceFrequency = RecurrenceFrequency.MONTHLY;
    }
  }

  /**
   * Get category description helper text
   */
  getCategoryHelp(category: ExpenseCategory): string {
    const helpTexts: Partial<Record<ExpenseCategory, string>> = {
      [ExpenseCategory.MAINTENANCE]: 'Regular maintenance and minor repairs',
      [ExpenseCategory.UTILITIES]: 'Electricity, water, gas, internet, etc.',
      [ExpenseCategory.CLEANING]: 'Cleaning services and supplies',
      [ExpenseCategory.SECURITY]: 'Security services, cameras, access systems',
      [ExpenseCategory.INSURANCE]: 'Building and liability insurance',
      [ExpenseCategory.TAXES]: 'Property taxes and government fees',
      [ExpenseCategory.MANAGEMENT]: 'Property management fees',
      [ExpenseCategory.REPAIRS]: 'Major repairs and renovations',
      [ExpenseCategory.LANDSCAPING]: 'Garden maintenance and outdoor areas',
      [ExpenseCategory.ELEVATOR]: 'Elevator maintenance and repairs',
      [ExpenseCategory.SUPPLIES]: 'Office and general supplies',
      [ExpenseCategory.LEGAL]: 'Legal consultations and fees',
      [ExpenseCategory.ACCOUNTING]: 'Bookkeeping and accounting services',
      [ExpenseCategory.MARKETING]: 'Advertising for vacant units',
      [ExpenseCategory.OTHER]: 'Any other expenses not listed above'
    };
    
    return helpTexts[category] || '';
  }

  /**
   * Format amount for display
   */
  formatAmount(value: number): string {
    return value ? value.toFixed(2) : '0.00';
  }
}