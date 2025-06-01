import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil, EMPTY, of } from 'rxjs';

// Services
import { ContractService } from '../../../shared/services/contract.service';
import { FlatService } from '../../../shared/services/flat.service';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';

// Models
import { ContractRequest, FlatResponse, ApartmentBuildingResponse } from '../../../shared/models';

// Components
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { FormErrorsComponent } from '../../../shared/components/form-errors/form-errors.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';

// Validators
import { dateRangeValidator } from '../../../shared/validators/date-range.validator';
import { phoneValidator } from '../../../shared/validators/phone.validator';

@Component({
  selector: 'app-contract-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    FormErrorsComponent,
    ButtonComponent
  ],
  templateUrl: './contract-form.component.html',
  styleUrl: './contract-form.component.scss'
})
export class ContractFormComponent implements OnInit, OnDestroy {
  contractForm!: FormGroup;
  buildings: ApartmentBuildingResponse[] = [];
  availableFlats: FlatResponse[] = [];
  selectedBuilding: ApartmentBuildingResponse | null = null;
  
  // Loading states
  isLoadingBuildings = false;
  isLoadingFlats = false;
  isCheckingOverlap = false;
  isSubmitting = false;
  
  // Error states
  overlapError = false;
  formErrors: string[] = [];
  
  // Due preview calculation
  duePreview: Array<{month: string; amount: number}> = [];
  showDuePreview = false;
  
  private destroy$ = new Subject<void>();
  private overlapCheck$ = new Subject<void>();
  
  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private contractService: ContractService,
    private flatService: FlatService,
    private buildingService: ApartmentBuildingService,
    private notificationService: NotificationService,
    public loadingService: LoadingService
  ) {}
  
  ngOnInit(): void {
    this.initializeForm();
    this.loadBuildings();
    this.setupFormSubscriptions();
    this.setupOverlapChecking();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Initialize the contract form with all sections
   */
  private initializeForm(): void {
    this.contractForm = this.fb.group({
      // Flat selection
      buildingId: [null, Validators.required],
      flatId: [null, Validators.required],
      
      // Contract period
      dateRange: this.fb.group({
        startDate: [null, Validators.required],
        endDate: [null, Validators.required]
      }, { validators: dateRangeValidator }),
      
      // Financial details
      monthlyRent: [null, [Validators.required, Validators.min(0.01)]],
      dayOfMonth: [1, [Validators.required, Validators.min(1), Validators.max(31)]],
      securityDeposit: [0, [Validators.min(0)]],
      
      // Tenant information
      tenantInfo: this.fb.group({
        name: ['', [Validators.required, Validators.maxLength(100)]],
        contact: ['', [phoneValidator]],
        email: ['', [Validators.email, Validators.maxLength(100)]]
      }),
      
      // Additional options
      notes: ['', Validators.maxLength(1000)],
      generateDuesImmediately: [true]
    });
  }
  
  /**
   * Load available buildings for the current user
   */
  private loadBuildings(): void {
    this.isLoadingBuildings = true;
    this.buildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings) => {
          this.buildings = buildings;
          this.isLoadingBuildings = false;
        },
        error: (error) => {
          console.error('Error loading buildings:', error);
          this.notificationService.error('Failed to load buildings');
          this.isLoadingBuildings = false;
        }
      });
  }
  
  /**
   * Load available flats when building is selected
   */
  private loadFlats(buildingId: number): void {
    this.isLoadingFlats = true;
    this.availableFlats = [];
    this.contractForm.patchValue({ flatId: null });
    
    this.flatService.getFlatsByBuilding(buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (flats) => {
          // Filter only active flats without active contracts
          this.availableFlats = flats.filter(flat => 
            flat.isActive && !flat.hasActiveContract
          );
          this.isLoadingFlats = false;
          
          if (this.availableFlats.length === 0) {
            this.notificationService.warning('No available flats in this building');
          }
        },
        error: (error) => {
          console.error('Error loading flats:', error);
          this.notificationService.error('Failed to load flats');
          this.isLoadingFlats = false;
        }
      });
  }
  
  /**
   * Setup form value change subscriptions
   */
  private setupFormSubscriptions(): void {
    // Building selection
    this.contractForm.get('buildingId')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(buildingId => {
        if (buildingId) {
          this.selectedBuilding = this.buildings.find(b => b.id === buildingId) || null;
          this.loadFlats(buildingId);
        }
      });
    
    // Auto-calculate security deposit as one month's rent
    this.contractForm.get('monthlyRent')?.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe(rent => {
        if (rent && this.contractForm.get('securityDeposit')?.value === 0) {
          this.contractForm.patchValue({ securityDeposit: rent }, { emitEvent: false });
        }
      });
    
    // Due preview calculation on relevant changes
    const previewTriggers = [
      'dateRange.startDate',
      'dateRange.endDate',
      'monthlyRent',
      'dayOfMonth'
    ];
    
    previewTriggers.forEach(path => {
      this.contractForm.get(path)?.valueChanges
        .pipe(
          debounceTime(500),
          takeUntil(this.destroy$)
        )
        .subscribe(() => this.calculateDuePreview());
    });
  }
  
  /**
   * Setup overlap checking with debounce
   * Note: Since backend checks overlaps on submission, this is optional
   * We can implement it later if we add a dedicated endpoint
   */
  private setupOverlapChecking(): void {
    // For now, we'll skip the real-time overlap checking
    // The backend will validate and return an error if there's an overlap
    // This avoids the 404 error for the missing endpoint
  }
  
  /**
   * Calculate and display due preview
   */
  private calculateDuePreview(): void {
    const startDate = this.contractForm.get('dateRange.startDate')?.value;
    const endDate = this.contractForm.get('dateRange.endDate')?.value;
    const monthlyRent = this.contractForm.get('monthlyRent')?.value;
    const dayOfMonth = this.contractForm.get('dayOfMonth')?.value;
    
    if (!startDate || !endDate || !monthlyRent || !dayOfMonth) {
      this.duePreview = [];
      this.showDuePreview = false;
      return;
    }
    
    // Calculate monthly dues
    this.duePreview = [];
    const start = new Date(startDate);
    const end = new Date(endDate);
    let currentDate = new Date(start);
    
    while (currentDate <= end) {
      const monthName = currentDate.toLocaleDateString('en-US', { 
        month: 'long', 
        year: 'numeric' 
      });
      
      this.duePreview.push({
        month: monthName,
        amount: monthlyRent
      });
      
      // Move to next month
      currentDate.setMonth(currentDate.getMonth() + 1);
    }
    
    this.showDuePreview = this.duePreview.length > 0;
  }
  
  /**
   * Submit the contract form
   */
  onSubmit(): void {
    if (this.contractForm.invalid || this.overlapError || this.isSubmitting) {
      this.contractForm.markAllAsTouched();
      this.formErrors = this.getFormErrors();
      return;
    }
    
    this.isSubmitting = true;
    this.formErrors = [];
    
    const formValue = this.contractForm.value;
    const contractRequest: ContractRequest = {
      flatId: formValue.flatId,
      startDate: this.formatDate(formValue.dateRange.startDate),
      endDate: this.formatDate(formValue.dateRange.endDate),
      monthlyRent: formValue.monthlyRent,
      dayOfMonth: formValue.dayOfMonth,
      securityDeposit: formValue.securityDeposit,
      tenantName: formValue.tenantInfo.name.trim(),
      tenantContact: formValue.tenantInfo.contact?.trim() || undefined,
      tenantEmail: formValue.tenantInfo.email?.trim() || undefined,
      notes: formValue.notes?.trim() || undefined,
      generateDuesImmediately: formValue.generateDuesImmediately
    };
    
    this.contractService.createContract(contractRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (contract) => {
          this.notificationService.success('Contract created successfully');
          // Navigate to contract details
          this.router.navigate(['../contracts', contract.id], { relativeTo: this.route });
        },
        error: (error: any) => {
          console.error('Error creating contract:', error);
          this.isSubmitting = false;
          
          if (error.error?.message) {
            this.formErrors = [error.error.message];
            // Check if it's an overlap error
            if (error.error.message.includes('overlap')) {
              this.overlapError = true;
              this.contractForm.get('dateRange')?.setErrors({ overlap: true });
            }
          } else if (error.error?.errors) {
            this.formErrors = Object.values(error.error.errors).flat() as string[];
          } else {
            this.formErrors = ['Failed to create contract. Please try again.'];
          }
        }
      });
  }
  
  /**
   * Cancel and navigate back
   */
  onCancel(): void {
    this.router.navigate(['../contracts'], { relativeTo: this.route });
  }
  
  /**
   * Format date to ISO string (YYYY-MM-DD)
   */
  private formatDate(date: Date | string): string {
    if (typeof date === 'string') {
      return date;
    }
    const d = new Date(date);
    return d.toISOString().split('T')[0];
  }
  
  /**
   * Get form validation errors
   */
  private getFormErrors(): string[] {
    const errors: string[] = [];
    
    if (this.contractForm.hasError('dateRange')) {
      errors.push('End date must be after start date');
    }
    
    if (this.overlapError) {
      errors.push('Selected dates overlap with an existing contract');
    }
    
    // Field-specific errors
    const fieldErrors: { [key: string]: string } = {
      buildingId: 'Please select a building',
      flatId: 'Please select a flat',
      'dateRange.startDate': 'Start date is required',
      'dateRange.endDate': 'End date is required',
      monthlyRent: 'Monthly rent is required and must be greater than 0',
      dayOfMonth: 'Payment day must be between 1 and 31',
      'tenantInfo.name': 'Tenant name is required',
      'tenantInfo.contact': 'Invalid phone number format',
      'tenantInfo.email': 'Invalid email format'
    };
    
    Object.entries(fieldErrors).forEach(([path, message]) => {
      const control = this.contractForm.get(path);
      if (control?.invalid && control?.touched) {
        errors.push(message);
      }
    });
    
    return [...new Set(errors)]; // Remove duplicates
  }
  
  /**
   * Get form control for template access
   */
  get f() {
    return this.contractForm.controls;
  }
  
  /**
   * Get date range group for template access
   */
  get dateRangeGroup() {
    return this.contractForm.get('dateRange') as FormGroup;
  }
  
  /**
   * Get tenant info group for template access
   */
  get tenantInfoGroup() {
    return this.contractForm.get('tenantInfo') as FormGroup;
  }
  
  /**
   * Calculate total due amount for preview
   */
  get totalDueAmount(): number {
    return this.duePreview.reduce((sum, due) => sum + due.amount, 0);
  }
  
  /**
   * Check if form has any errors
   */
  get hasErrors(): boolean {
    return this.contractForm.invalid || this.overlapError || this.formErrors.length > 0;
  }
  
  /**
   * Get today's date in YYYY-MM-DD format
   */
  get today(): string {
    return new Date().toISOString().split('T')[0];
  }
  
  /**
   * Calculate contract duration in months
   */
  calculateDuration(): number {
    const startDate = this.contractForm.get('dateRange.startDate')?.value;
    const endDate = this.contractForm.get('dateRange.endDate')?.value;
    
    if (!startDate || !endDate) {
      return 0;
    }
    
    const start = new Date(startDate);
    const end = new Date(endDate);
    
    // Calculate difference in months
    const yearDiff = end.getFullYear() - start.getFullYear();
    const monthDiff = end.getMonth() - start.getMonth();
    const dayDiff = end.getDate() - start.getDate();
    
    let totalMonths = yearDiff * 12 + monthDiff;
    
    // Add one more month if the day difference is positive
    if (dayDiff > 0) {
      totalMonths++;
    }
    
    return Math.max(0, totalMonths);
  }
  
  /**
   * Get selected flat number for display
   */
  get selectedFlatNumber(): string {
    const flatId = this.contractForm.get('flatId')?.value;
    if (!flatId) {
      return '-';
    }
    
    const flat = this.availableFlats.find(f => f.id === flatId);
    return flat?.flatNumber || '-';
  }
  
  /**
   * Get monthly rent value for display
   */
  get monthlyRentValue(): number {
    return this.contractForm.get('monthlyRent')?.value || 0;
  }
}