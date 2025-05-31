import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// Shared components
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';

// Services
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { NotificationService } from '../../../core/services/notification.service';

// Models
import { ApartmentBuildingRequest, ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';

@Component({
  selector: 'app-apartment-building-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    LoadingSpinnerComponent,
    FormErrorsComponent
  ],
  templateUrl: './apartment-building-form.component.html',
  styleUrls: ['./apartment-building-form.component.scss']
})
export class ApartmentBuildingFormComponent implements OnInit, OnDestroy {
  // Service injections
  private apartmentBuildingService = inject(ApartmentBuildingService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Form state
  formData: ApartmentBuildingRequest = {
    name: '',
    address: ''
  };

  // Component state
  isEditMode = false;
  buildingId: number | null = null;
  isLoading = false;
  isSubmitting = false;
  pageTitle = 'Create New Building';

  // Cleanup subject
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Check if we're in edit mode
    this.route.params
      .pipe(takeUntil(this.destroy$))
      .subscribe(params => {
        if (params['id']) {
          this.isEditMode = true;
          this.buildingId = +params['id'];
          this.pageTitle = 'Edit Building';
          this.loadBuilding();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load building data for editing
   */
  private loadBuilding(): void {
    if (!this.buildingId) return;

    this.isLoading = true;
    
    this.apartmentBuildingService.getBuilding(this.buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building) => {
          // Populate form with building data
          this.formData = {
            name: building.name,
            address: building.address || ''
          };
          this.isLoading = false;
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Error loading building:', error);
          this.notificationService.error('Failed to load building details');
          // Navigate back to list on error
          this.router.navigate(['/dashboard/buildings']);
        }
      });
  }

  /**
   * Submit form - create or update building
   */
  onSubmit(form: NgForm): void {
    if (form.invalid) {
      // Mark all fields as touched to show validation errors
      Object.keys(form.controls).forEach(key => {
        form.controls[key].markAsTouched();
      });
      return;
    }

    this.isSubmitting = true;

    const request$ = this.isEditMode && this.buildingId
      ? this.apartmentBuildingService.updateBuilding(this.buildingId, this.formData)
      : this.apartmentBuildingService.createBuilding(this.formData);

    request$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (building) => {
          const message = this.isEditMode 
            ? `Building "${building.name}" updated successfully`
            : `Building "${building.name}" created successfully`;
          
          this.notificationService.success(message);
          this.isSubmitting = false;
          
          // Navigate back to list
          this.router.navigate(['/dashboard/buildings']);
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error saving building:', error);
          // Error notification handled by service
        }
      });
  }

  /**
   * Cancel and navigate back
   */
  onCancel(): void {
    this.router.navigate(['/dashboard/buildings']);
  }

}