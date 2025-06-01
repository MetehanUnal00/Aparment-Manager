import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { FlatRequest, FlatResponse } from '../../../shared/models/flat.model';

/**
 * Component for creating and editing flats
 * Handles form validation and submission for flat management
 */
@Component({
  selector: 'app-flat-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    LoadingSpinnerComponent,
    ButtonComponent,
    FormErrorsComponent
  ],
  templateUrl: './flat-form.component.html',
  styleUrls: ['./flat-form.component.scss']
})
export class FlatFormComponent implements OnInit {
  // Inject services
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly flatService = inject(FlatService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /**
   * Form data model
   */
  formData: FlatRequest = {
    flatNumber: '',
    numberOfRooms: undefined,
    areaSqMeters: undefined,
    apartmentBuildingId: 0,
    tenantName: '',
    tenantContact: '',
    tenantEmail: '',
    monthlyRent: undefined,
    securityDeposit: undefined,
    tenantMoveInDate: undefined,
    isActive: true
  };

  /**
   * Available buildings for selection
   */
  buildings: ApartmentBuildingResponse[] = [];

  /**
   * Loading state
   */
  isLoading = true;

  /**
   * Form submission state
   */
  isSubmitting = false;

  /**
   * Edit mode flag
   */
  isEditMode = false;

  /**
   * Current flat ID (in edit mode)
   */
  flatId: number | null = null;

  /**
   * Building ID from route
   */
  buildingId: number | null = null;

  /**
   * Page title
   */
  get pageTitle(): string {
    return this.isEditMode ? 'Edit Flat' : 'Create New Flat';
  }

  ngOnInit(): void {
    // Get building ID from query params
    this.route.queryParams.subscribe(params => {
      if (params['buildingId']) {
        this.buildingId = +params['buildingId'];
        this.formData.apartmentBuildingId = this.buildingId;
      }
    });

    // Check if we're in edit mode
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.isEditMode = true;
        this.flatId = +params['id'];
      }
      this.loadData();
    });
  }

  /**
   * Load initial data
   */
  private loadData(): void {
    this.loadBuildings();
  }

  /**
   * Load buildings for selection
   */
  private loadBuildings(): void {
    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        
        if (this.isEditMode && this.flatId && this.buildingId) {
          this.loadFlat();
        } else {
          this.isLoading = false;
        }
      },
      error: (error) => {
        this.notification.error('Failed to load buildings. Please try again.');
        this.isLoading = false;
        console.error('Error loading buildings:', error);
      }
    });
  }

  /**
   * Load flat data for editing
   */
  private loadFlat(): void {
    if (!this.flatId || !this.buildingId) return;

    this.flatService.getFlat(this.buildingId, this.flatId).subscribe({
      next: (flat) => {
        this.populateForm(flat);
        this.isLoading = false;
      },
      error: (error) => {
        this.notification.error('Failed to load flat details. Please try again.');
        this.isLoading = false;
        console.error('Error loading flat:', error);
      }
    });
  }

  /**
   * Populate form with flat data
   */
  private populateForm(flat: FlatResponse): void {
    this.formData = {
      flatNumber: flat.flatNumber,
      numberOfRooms: flat.numberOfRooms,
      areaSqMeters: flat.areaSqMeters,
      apartmentBuildingId: flat.apartmentBuildingId,
      tenantName: flat.tenantName || '',
      tenantContact: flat.tenantContact || '',
      tenantEmail: flat.tenantEmail || '',
      monthlyRent: flat.monthlyRent,
      securityDeposit: flat.securityDeposit,
      tenantMoveInDate: flat.tenantMoveInDate ? this.formatDateForInput(flat.tenantMoveInDate) : undefined,
      isActive: flat.isActive
    };
  }

  /**
   * Format date for input field
   */
  private formatDateForInput(dateString: string): string {
    const date = new Date(dateString);
    return date.toISOString().split('T')[0];
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

    // Prepare the request data
    const requestData: FlatRequest = {
      ...this.formData,
      // Ensure empty strings are converted to undefined for optional fields
      tenantName: this.formData.tenantName || undefined,
      tenantContact: this.formData.tenantContact || undefined,
      tenantEmail: this.formData.tenantEmail || undefined,
      monthlyRent: this.formData.monthlyRent || undefined,
      securityDeposit: this.formData.securityDeposit || undefined,
      // Convert date string to LocalDateTime format (add time component)
      tenantMoveInDate: this.formData.tenantMoveInDate 
        ? `${this.formData.tenantMoveInDate}T00:00:00` 
        : undefined
    };

    if (this.isEditMode && this.flatId && this.buildingId) {
      // Update existing flat
      this.flatService.updateFlat(this.buildingId, this.flatId, requestData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/dashboard/flats'], { queryParams: { buildingId: this.buildingId } });
        },
        error: (error) => {
          this.isSubmitting = false;
          this.notification.error('Failed to update flat. Please try again.');
          console.error('Error updating flat:', error);
        }
      });
    } else {
      // Create new flat
      this.flatService.createFlat(requestData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.router.navigate(['/dashboard/flats'], { queryParams: { buildingId: this.formData.apartmentBuildingId } });
        },
        error: (error) => {
          this.isSubmitting = false;
          this.notification.error('Failed to create flat. Please try again.');
          console.error('Error creating flat:', error);
        }
      });
    }
  }

  /**
   * Handle cancel action
   */
  onCancel(): void {
    const queryParams = this.buildingId ? { buildingId: this.buildingId } : {};
    this.router.navigate(['/dashboard/flats'], { queryParams });
  }

  /**
   * Calculate today's date for move-in date validation
   */
  get today(): string {
    return new Date().toISOString().split('T')[0];
  }

  /**
   * Handle building change
   */
  onBuildingChange(): void {
    this.buildingId = this.formData.apartmentBuildingId;
  }

  /**
   * Check if tenant fields have any value
   */
  get hasTenantInfo(): boolean {
    return !!(this.formData.tenantName || this.formData.tenantContact || this.formData.tenantEmail);
  }

  /**
   * Clear all tenant information
   */
  clearTenantInfo(): void {
    this.formData.tenantName = '';
    this.formData.tenantContact = '';
    this.formData.tenantEmail = '';
    this.formData.monthlyRent = undefined;
    this.formData.securityDeposit = undefined;
    this.formData.tenantMoveInDate = undefined;
  }
}