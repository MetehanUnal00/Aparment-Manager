import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, switchMap, takeUntil, combineLatest, BehaviorSubject, of } from 'rxjs';
import { Router } from '@angular/router';

// Shared imports
import { 
  ContractSummaryResponse, 
  ContractStatus, 
  ContractSearchParams,
  PaginatedResponse,
  PaginationParams,
  getContractStatusColor,
  formatContractPeriod,
  isCurrentlyActive
} from '../../../shared/models';
import { 
  ContractService, 
  ApartmentBuildingService
} from '../../../shared/services';
import { AuthService } from '../../../auth/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { 
  LoadingSpinnerComponent,
  ButtonComponent,
  EmptyStateComponent,
  SearchBoxComponent,
  ConfirmDialogComponent,
  CurrencyFormatPipe,
  ResponsiveTableDirective
} from '../../../shared';

/**
 * Contract List Component
 * Displays contracts with search, filtering, and pagination
 * Supports actions based on contract state and user permissions
 */
@Component({
  selector: 'app-contract-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    ButtonComponent,
    EmptyStateComponent,
    SearchBoxComponent,
    ConfirmDialogComponent,
    CurrencyFormatPipe,
    ResponsiveTableDirective
  ],
  templateUrl: './contract-list.component.html',
  styleUrl: './contract-list.component.scss'
})
export class ContractListComponent implements OnInit, OnDestroy {
  // Service injections
  private fb = inject(FormBuilder);
  private contractService = inject(ContractService);
  private buildingService = inject(ApartmentBuildingService);
  private notificationService = inject(NotificationService);
  private authService = inject(AuthService);
  private router = inject(Router);

  // Component state
  contracts$ = new BehaviorSubject<PaginatedResponse<ContractSummaryResponse> | null>(null);
  buildings$ = this.buildingService.getBuildings();
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string>('');
  
  // User permissions
  currentUser$ = this.authService.currentUser;
  canManageContracts = false; // Will be set based on user role

  // Pagination state
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;

  // Search and filter form
  filterForm!: FormGroup;
  private searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();

  // Contract status options for filter
  contractStatuses = Object.values(ContractStatus);
  
  // UI state
  selectedContracts = new Set<number>();
  showFilters = false;
  
  // Export enum and Math for template
  ContractStatus = ContractStatus;
  Math = Math;

  ngOnInit(): void {
    // Initialize form
    this.initializeForm();
    
    // Set user permissions
    this.authService.currentUser.pipe(
      takeUntil(this.destroy$)
    ).subscribe(user => {
      this.canManageContracts = user?.roles?.includes('ADMIN') || user?.roles?.includes('MANAGER') || false;
    });

    // Set up search with debounce
    this.searchSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(searchTerm => {
      this.filterForm.patchValue({ tenantName: searchTerm });
      this.loadContracts();
    });

    // Load contracts when filters change
    this.filterForm.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      takeUntil(this.destroy$)
    ).subscribe(() => {
      this.currentPage = 0; // Reset to first page
      this.loadContracts();
    });

    // Initial load
    this.loadContracts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize the filter form
   */
  private initializeForm(): void {
    this.filterForm = this.fb.group({
      buildingId: [null],
      status: [null],
      tenantName: [''],
      expiringWithinDays: [null],
      hasOverdueDues: [false]
    });
  }

  /**
   * Load contracts based on current filters and pagination
   */
  loadContracts(): void {
    this.loading$.next(true);
    this.error$.next('');

    const filters = this.getFilterParams();
    const pageParams: PaginationParams = {
      page: this.currentPage,
      size: this.pageSize,
      sort: 'startDate,desc'
    };

    // If user is a manager and no building is selected, don't load
    this.currentUser$.pipe(
      switchMap(user => {
        if (user?.roles?.includes('MANAGER') && !user?.roles?.includes('ADMIN') && !filters.buildingId) {
          this.notificationService.warning('Please select a building to view contracts');
          this.loading$.next(false);
          return [];
        }

        // Use building-specific endpoint if building is selected
        if (filters.buildingId) {
          return this.contractService.getContractsByBuilding(
            filters.buildingId,
            pageParams,
            { forceRefresh: false, enablePolling: false }
          );
        } else if (filters.tenantName) {
          // Use search endpoint only when there's a search term
          return this.contractService.searchContracts(filters, pageParams);
        } else {
          // For admin users without a building selected and no search term,
          // we need to show a message or load from first building
          this.notificationService.info('Please select a building or search by tenant name to view contracts');
          this.loading$.next(false);
          return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: pageParams.size, first: true, last: true, empty: true } as PaginatedResponse<ContractSummaryResponse>);
        }
      }),
      takeUntil(this.destroy$)
    ).subscribe({
      next: (response: PaginatedResponse<ContractSummaryResponse>) => {
        this.contracts$.next(response);
        this.totalElements = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading$.next(false);
      },
      error: (error: any) => {
        console.error('Error loading contracts:', error);
        this.error$.next('Failed to load contracts. Please try again.');
        this.loading$.next(false);
      }
    });
  }

  /**
   * Get filter parameters from form
   */
  private getFilterParams(): ContractSearchParams {
    const formValue = this.filterForm.value;
    const params: ContractSearchParams = {};

    if (formValue.buildingId) {
      params.buildingId = formValue.buildingId;
    }
    if (formValue.status) {
      params.status = formValue.status;
    }
    if (formValue.tenantName) {
      params.tenantName = formValue.tenantName;
    }
    if (formValue.expiringWithinDays) {
      params.expiringWithinDays = formValue.expiringWithinDays;
    }
    if (formValue.hasOverdueDues) {
      params.hasOverdueDues = formValue.hasOverdueDues;
    }

    return params;
  }

  /**
   * Handle search input
   */
  onSearch(searchTerm: string): void {
    this.searchSubject.next(searchTerm);
  }

  /**
   * Clear all filters
   */
  clearFilters(): void {
    this.filterForm.reset({
      buildingId: null,
      status: null,
      tenantName: '',
      expiringWithinDays: null,
      hasOverdueDues: false
    });
    this.searchSubject.next('');
  }

  /**
   * Toggle filter panel visibility
   */
  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  /**
   * Handle page change
   */
  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadContracts();
  }

  /**
   * Handle page size change
   */
  onPageSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0; // Reset to first page
    this.loadContracts();
  }

  /**
   * Navigate to contract details
   */
  viewContract(contractId: number): void {
    this.router.navigate(['/dashboard/contracts', contractId]);
  }

  /**
   * Navigate to create new contract
   */
  createContract(): void {
    this.router.navigate(['/dashboard/contracts/new']);
  }

  /**
   * Navigate to renew contract
   */
  renewContract(contract: ContractSummaryResponse): void {
    this.router.navigate(['/dashboard/contracts', contract.id, 'renew']);
  }

  /**
   * Navigate to modify contract
   */
  modifyContract(contract: ContractSummaryResponse): void {
    this.router.navigate(['/dashboard/contracts', contract.id, 'modify']);
  }

  /**
   * Show cancel contract dialog
   */
  cancelContract(contract: ContractSummaryResponse): void {
    // Navigate to cancel dialog/form
    this.router.navigate(['/dashboard/contracts', contract.id, 'cancel']);
  }

  /**
   * Get status badge CSS class
   */
  getStatusBadgeClass(status: ContractStatus): string {
    const color = getContractStatusColor(status);
    return `badge badge-${color}`;
  }

  /**
   * Format contract period for display
   */
  formatPeriod(startDate: string, endDate: string): string {
    return formatContractPeriod(startDate, endDate);
  }

  /**
   * Check if contract is currently active
   */
  isActive(contract: ContractSummaryResponse): boolean {
    return isCurrentlyActive(contract);
  }

  /**
   * Get row CSS class based on contract state
   */
  getRowClass(contract: ContractSummaryResponse): string {
    const classes: string[] = [];
    
    if (contract.isExpiringSoon) {
      classes.push('expiring-soon');
    }
    if (contract.hasOverdueDues) {
      classes.push('has-overdue');
    }
    if (!this.isActive(contract)) {
      classes.push('inactive');
    }
    
    return classes.join(' ');
  }

  /**
   * Check if contract can be renewed
   */
  canRenew(contract: ContractSummaryResponse): boolean {
    return this.canManageContracts && 
           contract.status === ContractStatus.ACTIVE && 
           contract.isExpiringSoon;
  }

  /**
   * Check if contract can be modified
   */
  canModify(contract: ContractSummaryResponse): boolean {
    return this.canManageContracts && 
           this.isActive(contract);
  }

  /**
   * Check if contract can be cancelled
   */
  canCancel(contract: ContractSummaryResponse): boolean {
    return this.canManageContracts && 
           (contract.status === ContractStatus.ACTIVE || 
            contract.status === ContractStatus.PENDING);
  }

  /**
   * Get expiry badge text
   */
  getExpiryBadge(contract: ContractSummaryResponse): string | null {
    if (!contract.daysUntilExpiry || contract.daysUntilExpiry > 30) {
      return null;
    }
    
    if (contract.daysUntilExpiry <= 0) {
      return 'Expired';
    } else if (contract.daysUntilExpiry <= 7) {
      return `${contract.daysUntilExpiry} days`;
    } else {
      return `${contract.daysUntilExpiry} days`;
    }
  }

  /**
   * Get expiry badge CSS class
   */
  getExpiryBadgeClass(contract: ContractSummaryResponse): string {
    if (!contract.daysUntilExpiry) return '';
    
    if (contract.daysUntilExpiry <= 0) {
      return 'badge badge-danger';
    } else if (contract.daysUntilExpiry <= 7) {
      return 'badge badge-danger';
    } else if (contract.daysUntilExpiry <= 30) {
      return 'badge badge-warning';
    }
    return '';
  }

  /**
   * TrackBy function for ngFor optimization
   */
  trackByContractId(index: number, contract: ContractSummaryResponse): number {
    return contract.id;
  }

  /**
   * Get empty state message based on current filters
   */
  getEmptyStateMessage(): string {
    const filters = this.getFilterParams();
    
    if (!filters.buildingId && !filters.tenantName) {
      return 'Please select a building or search by tenant name to view contracts';
    } else if (filters.tenantName) {
      return `No contracts found for tenant "${filters.tenantName}"`;
    } else if (this.filterForm.dirty) {
      return 'No contracts match your filters. Try adjusting them.';
    } else {
      return 'No contracts found for this building. Create your first contract to get started.';
    }
  }
}