import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { PaymentService } from '../../../shared/services/payment.service';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';
import { 
  PaymentRequest, 
  PaymentResponse, 
  PaymentMethod 
} from '../../../shared/models/payment.model';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { FlatResponse } from '../../../shared/models/flat.model';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ValidationErrors } from '@angular/forms';

@Component({
  selector: 'app-payment-form',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    FormErrorsComponent,
    LoadingSpinnerComponent
  ],
  templateUrl: './payment-form.component.html',
  styleUrls: ['./payment-form.component.scss']
})
export class PaymentFormComponent implements OnInit {
  // Service injections
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private buildingService = inject(ApartmentBuildingService);
  private flatService = inject(FlatService);
  private notificationService = inject(NotificationService);
  private loadingService = inject(LoadingService);

  // Component properties
  paymentId: number | null = null;
  isEditMode = false;
  
  // Form data
  payment: PaymentRequest = {
    flatId: 0,
    amount: 0,
    paymentDate: new Date().toISOString().split('T')[0], // Format for HTML date input
    description: '',
    paymentMethod: PaymentMethod.CASH
  };

  // Lists for dropdowns
  buildings: ApartmentBuildingResponse[] = [];
  flats: FlatResponse[] = [];
  selectedBuildingId: number | null = null;
  
  // Payment methods for dropdown
  paymentMethods = Object.values(PaymentMethod);
  
  // Form validation
  formErrors: Record<string, ValidationErrors> = {};
  
  // Loading states
  loadingBuildings = false;
  loadingFlats = false;
  loadingPayment = false;
  submitting = false;

  // Current date for date picker max attribute
  currentDate = new Date().toISOString().split('T')[0];

  ngOnInit(): void {
    // Check if we're in edit mode
    this.route.params.subscribe(params => {
      if (params['id']) {
        this.paymentId = +params['id'];
        this.isEditMode = true;
        this.loadPayment();
      }
    });

    // Check for query parameters (e.g., pre-selected flat)
    this.route.queryParams.subscribe(params => {
      if (params['flatId']) {
        this.payment.flatId = +params['flatId'];
        // Load the flat to get its building
        this.loadFlatAndBuilding(this.payment.flatId);
      } else if (params['buildingId']) {
        this.selectedBuildingId = +params['buildingId'];
        this.loadFlats();
      }
    });

    // Load buildings
    this.loadBuildings();
  }

  // Load all buildings for the dropdown
  loadBuildings(): void {
    this.loadingBuildings = true;
    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        this.loadingBuildings = false;
      },
      error: (error: any) => {
        this.notificationService.error('Failed to load buildings');
        this.loadingBuildings = false;
      }
    });
  }

  // Load flats for the selected building
  loadFlats(): void {
    if (!this.selectedBuildingId) {
      this.flats = [];
      return;
    }

    this.loadingFlats = true;
    this.flatService.getFlatsByBuilding(this.selectedBuildingId).subscribe({
      next: (flats) => {
        // Only show active flats
        this.flats = flats.filter(flat => flat.isActive);
        this.loadingFlats = false;
      },
      error: (error: any) => {
        this.notificationService.error('Failed to load flats');
        this.loadingFlats = false;
      }
    });
  }

  // Load flat and determine its building
  loadFlatAndBuilding(flatId: number): void {
    // We need to find which building contains this flat
    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        // Search through all buildings to find the flat
        for (const building of buildings) {
          this.flatService.getFlatsByBuilding(building.id).subscribe({
            next: (flats) => {
              const flat = flats.find(f => f.id === flatId);
              if (flat) {
                this.selectedBuildingId = building.id;
                this.payment.flatId = flatId;
                this.flats = flats.filter(f => f.isActive);
                return;
              }
            }
          });
        }
      },
      error: (error: any) => {
        this.notificationService.error('Failed to load flat details');
      }
    });
  }

  // Load existing payment for editing
  loadPayment(): void {
    if (!this.paymentId) return;

    this.loadingPayment = true;
    // We need to get the payment from the flat's payment list
    // Since there's no direct getPayment endpoint, we'll need to load from a flat
    this.notificationService.warning('Edit functionality requires payment lookup implementation');
    this.loadingPayment = false;
  }

  // Handle building selection change
  onBuildingChange(): void {
    this.payment.flatId = 0; // Reset flat selection
    this.loadFlats();
  }

  // Validate the form
  validateForm(): boolean {
    this.formErrors = {};
    let isValid = true;

    if (!this.payment.flatId || this.payment.flatId === 0) {
      this.formErrors['flatId'] = { required: true };
      isValid = false;
    }

    if (!this.payment.amount || this.payment.amount <= 0) {
      this.formErrors['amount'] = { min: true };
      isValid = false;
    }

    if (!this.payment.paymentDate) {
      this.formErrors['paymentDate'] = { required: true };
      isValid = false;
    }

    if (!this.payment.description || this.payment.description.trim().length === 0) {
      this.formErrors['description'] = { required: true };
      isValid = false;
    }

    if (!this.payment.paymentMethod) {
      this.formErrors['paymentMethod'] = { required: true };
      isValid = false;
    }

    return isValid;
  }

  // Submit the form
  onSubmit(): void {
    if (!this.validateForm()) {
      this.notificationService.error('Please fix the form errors');
      return;
    }

    this.submitting = true;

    // Convert date to LocalDateTime format expected by backend
    const paymentData: PaymentRequest = {
      ...this.payment,
      paymentDate: this.payment.paymentDate + 'T00:00:00'
    };

    const operation = this.isEditMode && this.paymentId
      ? this.paymentService.updatePayment(this.paymentId, paymentData)
      : this.paymentService.createPayment(paymentData);

    operation.subscribe({
      next: (response) => {
        this.notificationService.success(
          this.isEditMode ? 'Payment updated successfully' : 'Payment recorded successfully'
        );
        this.submitting = false;
        // Navigate back to payment list or flat details
        if (this.payment.flatId) {
          this.router.navigate(['/dashboard/flats', this.payment.flatId]);
        } else {
          this.router.navigate(['/dashboard/payments']);
        }
      },
      error: (error: any) => {
        this.submitting = false;
        // Error is already handled by the service
      }
    });
  }

  // Cancel and go back
  onCancel(): void {
    if (this.payment.flatId && !this.isEditMode) {
      this.router.navigate(['/dashboard/flats', this.payment.flatId]);
    } else {
      this.router.navigate(['/dashboard/payments']);
    }
  }

  // Get display name for payment method
  getPaymentMethodDisplay(method: PaymentMethod): string {
    const displayNames: Record<PaymentMethod, string> = {
      [PaymentMethod.CASH]: 'Cash',
      [PaymentMethod.BANK_TRANSFER]: 'Bank Transfer',
      [PaymentMethod.CREDIT_CARD]: 'Credit Card',
      [PaymentMethod.DEBIT_CARD]: 'Debit Card',
      [PaymentMethod.CHECK]: 'Check',
      [PaymentMethod.ONLINE_PAYMENT]: 'Online Payment',
      [PaymentMethod.OTHER]: 'Other'
    };
    return displayNames[method] || method;
  }

  // Get flat display name
  getFlatDisplay(flat: FlatResponse): string {
    const tenant = flat.activeContract?.tenantName ? ` - ${flat.activeContract.tenantName}` : ' - Vacant';
    return `${flat.flatNumber}${tenant}`;
  }
}