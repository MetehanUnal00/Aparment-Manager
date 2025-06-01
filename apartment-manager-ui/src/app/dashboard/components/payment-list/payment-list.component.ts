import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil, debounceTime, distinctUntilChanged } from 'rxjs';
import { PaymentService } from '../../../shared/services/payment.service';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';
import { 
  PaymentResponse, 
  PaymentMethod 
} from '../../../shared/models/payment.model';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { FlatResponse } from '../../../shared/models/flat.model';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';

@Component({
  selector: 'app-payment-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    SearchBoxComponent,
    ConfirmDialogComponent,
    CurrencyFormatPipe
  ],
  templateUrl: './payment-list.component.html',
  styleUrls: ['./payment-list.component.scss']
})
export class PaymentListComponent implements OnInit, OnDestroy {
  // Service injections
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private paymentService = inject(PaymentService);
  private buildingService = inject(ApartmentBuildingService);
  private flatService = inject(FlatService);
  private notificationService = inject(NotificationService);
  private loadingService = inject(LoadingService);

  // Component properties
  private destroy$ = new Subject<void>();
  
  // Data
  payments: PaymentResponse[] = [];
  filteredPayments: PaymentResponse[] = [];
  buildings: ApartmentBuildingResponse[] = [];
  flats: FlatResponse[] = [];
  
  // Filters
  selectedBuildingId: number | null = null;
  selectedFlatId: number | null = null;
  searchTerm = '';
  dateFrom = '';
  dateTo = '';
  selectedPaymentMethod: PaymentMethod | 'ALL' = 'ALL';
  
  // Payment methods for filter
  paymentMethods = Object.values(PaymentMethod);
  
  // Sorting
  sortColumn: 'date' | 'amount' | 'flat' | 'method' = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Loading states
  loadingPayments = false;
  loadingBuildings = false;
  loadingFlats = false;
  
  // Delete confirmation
  showDeleteConfirm = false;
  paymentToDelete: PaymentResponse | null = null;
  
  // Pagination
  currentPage = 1;
  pageSize = 20;
  totalPages = 1;

  ngOnInit(): void {
    // Load buildings
    this.loadBuildings();
    
    // Check for query parameters
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['buildingId']) {
        this.selectedBuildingId = +params['buildingId'];
        this.onBuildingChange();
      } else if (params['flatId']) {
        this.selectedFlatId = +params['flatId'];
        // Load the flat to get its building
        this.loadFlatAndBuilding(this.selectedFlatId);
      }
    });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Load all buildings
  loadBuildings(): void {
    this.loadingBuildings = true;
    this.buildingService.getBuildings().subscribe({
      next: (buildings: ApartmentBuildingResponse[]) => {
        this.buildings = buildings;
        this.loadingBuildings = false;
        
        // If we have a pre-selected building, load its data
        if (this.selectedBuildingId) {
          this.loadPayments();
        }
      },
      error: (error: any) => {
        this.notificationService.error('Failed to load buildings');
        this.loadingBuildings = false;
      }
    });
  }

  // Load flats for selected building
  loadFlats(): void {
    if (!this.selectedBuildingId) {
      this.flats = [];
      return;
    }

    this.loadingFlats = true;
    this.flatService.getFlatsByBuilding(this.selectedBuildingId).subscribe({
      next: (flats) => {
        this.flats = flats;
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
    this.loadingFlats = true;
    this.buildingService.getBuildings().subscribe({
      next: (buildings: ApartmentBuildingResponse[]) => {
        // Search through all buildings to find the flat
        let flatFound = false;
        let checkCount = 0;
        
        for (const building of buildings) {
          this.flatService.getFlatsByBuilding(building.id).subscribe({
            next: (flats: FlatResponse[]) => {
              checkCount++;
              const flat = flats.find(f => f.id === flatId);
              if (flat && !flatFound) {
                flatFound = true;
                this.selectedBuildingId = building.id;
                this.selectedFlatId = flatId;
                this.flats = flats;
                this.loadingFlats = false;
                this.loadPayments();
              } else if (checkCount === buildings.length && !flatFound) {
                this.loadingFlats = false;
                this.notificationService.error('Flat not found');
              }
            },
            error: (error: any) => {
              checkCount++;
              if (checkCount === buildings.length && !flatFound) {
                this.loadingFlats = false;
                this.notificationService.error('Failed to load flat details');
              }
            }
          });
        }
      },
      error: (error: any) => {
        this.loadingFlats = false;
        this.notificationService.error('Failed to load buildings');
      }
    });
  }

  // Load payments based on selected filters
  loadPayments(): void {
    if (!this.selectedBuildingId) {
      this.payments = [];
      this.filteredPayments = [];
      return;
    }

    this.loadingPayments = true;
    
    // Determine which endpoint to use
    const paymentObservable = this.selectedFlatId
      ? this.paymentService.getPaymentsByFlat(this.selectedFlatId)
      : this.paymentService.getPaymentsByBuilding(this.selectedBuildingId);

    paymentObservable.subscribe({
      next: (payments) => {
        this.payments = payments;
        this.applyFilters();
        this.loadingPayments = false;
      },
      error: (error: any) => {
        this.notificationService.error('Failed to load payments');
        this.loadingPayments = false;
      }
    });
  }

  // Handle building selection change
  onBuildingChange(): void {
    this.selectedFlatId = null; // Reset flat selection
    this.loadFlats();
    this.loadPayments();
  }

  // Handle flat selection change
  onFlatChange(): void {
    this.loadPayments();
  }

  // Apply filters and sorting
  applyFilters(): void {
    let filtered = [...this.payments];

    // Search filter
    if (this.searchTerm) {
      const search = this.searchTerm.toLowerCase();
      filtered = filtered.filter(payment => 
        (payment.description && payment.description.toLowerCase().includes(search)) ||
        payment.flat.flatNumber.toLowerCase().includes(search) ||
        (payment.flat.tenantName && payment.flat.tenantName.toLowerCase().includes(search)) ||
        payment.amount.toString().includes(search)
      );
    }

    // Date range filter
    if (this.dateFrom) {
      const fromDate = new Date(this.dateFrom);
      filtered = filtered.filter(payment => 
        new Date(payment.paymentDate) >= fromDate
      );
    }

    if (this.dateTo) {
      const toDate = new Date(this.dateTo);
      toDate.setHours(23, 59, 59, 999); // Include the entire end date
      filtered = filtered.filter(payment => 
        new Date(payment.paymentDate) <= toDate
      );
    }

    // Payment method filter
    if (this.selectedPaymentMethod !== 'ALL') {
      filtered = filtered.filter(payment => 
        payment.paymentMethod === this.selectedPaymentMethod
      );
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let comparison = 0;
      
      switch (this.sortColumn) {
        case 'date':
          comparison = new Date(a.paymentDate).getTime() - new Date(b.paymentDate).getTime();
          break;
        case 'amount':
          comparison = a.amount - b.amount;
          break;
        case 'flat':
          comparison = a.flat.flatNumber.localeCompare(b.flat.flatNumber);
          break;
        case 'method':
          comparison = a.paymentMethod.localeCompare(b.paymentMethod);
          break;
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });

    // Update filtered payments
    this.filteredPayments = filtered;
    
    // Update pagination
    this.totalPages = Math.ceil(this.filteredPayments.length / this.pageSize);
    this.currentPage = 1;
  }

  // Handle search
  onSearch(term: string): void {
    this.searchTerm = term;
    this.applyFilters();
  }

  // Handle sorting
  sort(column: 'date' | 'amount' | 'flat' | 'method'): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'desc';
    }
    this.applyFilters();
  }

  // Get paginated payments
  get paginatedPayments(): PaymentResponse[] {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    return this.filteredPayments.slice(start, end);
  }

  // Navigate to payment form
  createPayment(): void {
    const queryParams: any = {};
    if (this.selectedBuildingId) {
      queryParams.buildingId = this.selectedBuildingId;
    }
    if (this.selectedFlatId) {
      queryParams.flatId = this.selectedFlatId;
    }
    this.router.navigate(['/dashboard/payments/new'], { queryParams });
  }

  // Edit payment
  editPayment(payment: PaymentResponse): void {
    this.router.navigate(['/dashboard/payments', payment.id, 'edit']);
  }

  // Delete payment
  confirmDelete(payment: PaymentResponse): void {
    this.paymentToDelete = payment;
    this.showDeleteConfirm = true;
  }

  deletePayment(): void {
    if (!this.paymentToDelete) return;

    this.paymentService.deletePayment(this.paymentToDelete.id).subscribe({
      next: () => {
        this.notificationService.success('Payment deleted successfully');
        this.showDeleteConfirm = false;
        this.paymentToDelete = null;
        this.loadPayments(); // Reload payments
      },
      error: (error: any) => {
        this.showDeleteConfirm = false;
        // Error is already handled by the service
      }
    });
  }

  // Get payment method display name
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

  // Get payment method badge class
  getPaymentMethodClass(method: PaymentMethod): string {
    const classMap: Record<PaymentMethod, string> = {
      [PaymentMethod.CASH]: 'badge-success',
      [PaymentMethod.BANK_TRANSFER]: 'badge-info',
      [PaymentMethod.CREDIT_CARD]: 'badge-warning',
      [PaymentMethod.DEBIT_CARD]: 'badge-warning',
      [PaymentMethod.CHECK]: 'badge-secondary',
      [PaymentMethod.ONLINE_PAYMENT]: 'badge-primary',
      [PaymentMethod.OTHER]: 'badge-secondary'
    };
    return classMap[method] || 'badge-secondary';
  }

  // Reset filters
  resetFilters(): void {
    this.searchTerm = '';
    this.dateFrom = '';
    this.dateTo = '';
    this.selectedPaymentMethod = 'ALL';
    this.applyFilters();
  }

  // Check if there are no payments due to filters
  get hasNoSearchResults(): boolean {
    return this.payments.length > 0 && this.filteredPayments.length === 0;
  }

  // Check if there are active filters
  get hasActiveFilters(): boolean {
    return this.searchTerm !== '' || 
           this.dateFrom !== '' || 
           this.dateTo !== '' || 
           this.selectedPaymentMethod !== 'ALL';
  }

  // Calculate total amount of filtered payments
  getTotalAmount(): number {
    return this.filteredPayments.reduce((sum, payment) => sum + payment.amount, 0);
  }
}