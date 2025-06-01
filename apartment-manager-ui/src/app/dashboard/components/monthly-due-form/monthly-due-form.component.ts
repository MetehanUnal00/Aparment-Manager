import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// Services
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { FlatService } from '../../../shared/services/flat.service';
import { MonthlyDueService } from '../../../shared/services/monthly-due.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';

// Models
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { FlatResponse } from '../../../shared/models/flat.model';
import { MonthlyDueRequest, MonthlyDueResponse, DueStatus } from '../../../shared/models/monthly-due.model';

// Shared Components
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';

// Directives
import { AutofocusDirective } from '../../../shared/directives/autofocus.directive';

/**
 * Component for creating and editing individual monthly dues
 */
@Component({
  selector: 'app-monthly-due-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSpinnerComponent,
    ButtonComponent,
    FormErrorsComponent,
    AutofocusDirective
  ],
  templateUrl: './monthly-due-form.component.html',
  styleUrls: ['./monthly-due-form.component.scss']
})
export class MonthlyDueFormComponent implements OnInit, OnDestroy {
  // Inject services
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private buildingService = inject(ApartmentBuildingService);
  private flatService = inject(FlatService);
  private monthlyDueService = inject(MonthlyDueService);
  private notification = inject(NotificationService);
  loading = inject(LoadingService);

  // Component state
  isEditMode = false;
  dueId: number | null = null;
  buildings: ApartmentBuildingResponse[] = [];
  flats: FlatResponse[] = [];
  
  // Form model
  due: MonthlyDueRequest = {
    flatId: undefined,
    buildingId: undefined,
    dueAmount: 0,
    dueDate: this.getDefaultDueDate(),
    dueDescription: '',
    baseRent: 0,
    additionalCharges: 0,
    additionalChargesDescription: ''
  };

  // For edit mode
  existingDue: MonthlyDueResponse | null = null;
  selectedBuildingId: number | null = null;

  // Form validation
  errors: { [key: string]: string } = {};
  submitted = false;

  // Component lifecycle
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Check if we're in edit mode
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.dueId = parseInt(id, 10);
      // Note: Backend doesn't have GET by ID endpoint for monthly dues
      // In a real app, you'd need to implement this endpoint
      this.notification.warning('Edit functionality is limited - backend does not support fetching individual dues');
    }

    // Load buildings
    this.loadBuildings();

    // Check for pre-selected building from query params
    const buildingId = this.route.snapshot.queryParamMap.get('buildingId');
    if (buildingId) {
      this.selectedBuildingId = parseInt(buildingId, 10);
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all buildings
   */
  private loadBuildings(): void {
    this.buildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings: ApartmentBuildingResponse[]) => {
          this.buildings = buildings;
          
          // If pre-selected building, load its flats
          if (this.selectedBuildingId) {
            this.onBuildingChange();
          }
        },
        error: (error: any) => {
          console.error('Error loading buildings:', error);
          this.notification.error('Failed to load buildings');
        }
      });
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(): void {
    if (!this.selectedBuildingId) {
      this.flats = [];
      this.due.flatId = undefined;
      return;
    }

    // Load flats for selected building
    this.flatService.getFlatsByBuilding(this.selectedBuildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (flats) => {
          // Only show active flats
          this.flats = flats.filter(flat => flat.isActive);
          
          // Clear flat selection if it's not in the new list
          if (this.due.flatId && !this.flats.find(f => f.id === this.due.flatId)) {
            this.due.flatId = undefined;
          }
        },
        error: (error) => {
          console.error('Error loading flats:', error);
          this.notification.error('Failed to load flats');
        }
      });
  }

  /**
   * Handle flat selection change
   */
  onFlatChange(): void {
    if (!this.due.flatId) return;

    // Find selected flat to pre-fill rent amount
    const selectedFlat = this.flats.find(f => f.id === this.due.flatId);
    if (selectedFlat && selectedFlat.monthlyRent) {
      this.due.baseRent = selectedFlat.monthlyRent;
      this.updateTotalAmount();
    }
  }

  /**
   * Update total due amount
   */
  updateTotalAmount(): void {
    const baseRent = this.due.baseRent || 0;
    const additionalCharges = this.due.additionalCharges || 0;
    this.due.dueAmount = baseRent + additionalCharges;
  }

  /**
   * Validate form
   */
  private validateForm(): boolean {
    this.errors = {};

    if (!this.due.flatId) {
      this.errors['flatId'] = 'Please select a flat';
    }

    if (!this.due.dueAmount || this.due.dueAmount <= 0) {
      this.errors['dueAmount'] = 'Due amount must be greater than 0';
    }

    if (!this.due.dueDate) {
      this.errors['dueDate'] = 'Due date is required';
    } else {
      const dueDate = new Date(this.due.dueDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      
      if (dueDate < today) {
        this.errors['dueDate'] = 'Due date cannot be in the past';
      }
    }

    if (this.due.dueDescription && this.due.dueDescription.length > 500) {
      this.errors['dueDescription'] = 'Description cannot exceed 500 characters';
    }

    if (this.due.additionalChargesDescription && this.due.additionalChargesDescription.length > 500) {
      this.errors['additionalChargesDescription'] = 'Additional charges description cannot exceed 500 characters';
    }

    return Object.keys(this.errors).length === 0;
  }

  /**
   * Submit form
   */
  onSubmit(): void {
    this.submitted = true;

    if (!this.validateForm()) {
      this.notification.error('Please fix the errors in the form');
      return;
    }

    // Convert date to LocalDateTime format
    const dueDate = new Date(this.due.dueDate);
    const formattedDue = {
      ...this.due,
      dueDate: dueDate.toISOString().split('T')[0] + 'T00:00:00'
    };

    if (this.isEditMode && this.dueId) {
      // Update existing due
      this.monthlyDueService.updateMonthlyDue(this.dueId, formattedDue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.notification.success('Monthly due updated successfully');
            this.router.navigate(['/dashboard/monthly-dues']);
          },
          error: (error) => {
            console.error('Error updating due:', error);
            this.notification.error('Failed to update monthly due');
          }
        });
    } else {
      // Create new due
      this.monthlyDueService.createMonthlyDue(formattedDue)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.notification.success('Monthly due created successfully');
            this.router.navigate(['/dashboard/monthly-dues']);
          },
          error: (error) => {
            console.error('Error creating due:', error);
            if (error.error?.message?.includes('already exists')) {
              this.notification.error('A due already exists for this flat and month');
            } else {
              this.notification.error('Failed to create monthly due');
            }
          }
        });
    }
  }

  /**
   * Cancel and go back
   */
  cancel(): void {
    this.router.navigate(['/dashboard/monthly-dues']);
  }

  /**
   * Get default due date (1st of next month)
   */
  private getDefaultDueDate(): string {
    const today = new Date();
    const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 1);
    return nextMonth.toISOString().split('T')[0];
  }

  /**
   * Get minimum date for date picker (today)
   */
  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Get form title
   */
  get formTitle(): string {
    return this.isEditMode ? 'Edit Monthly Due' : 'Create Monthly Due';
  }

  /**
   * Check if flat is occupied
   */
  isFlatOccupied(flat: FlatResponse): boolean {
    return !!flat.tenantName;
  }

  /**
   * Get flat display name
   */
  getFlatDisplayName(flat: FlatResponse): string {
    let name = flat.flatNumber;
    if (flat.tenantName) {
      name += ` - ${flat.tenantName}`;
    } else {
      name += ' (Vacant)';
    }
    return name;
  }
}