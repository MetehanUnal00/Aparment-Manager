import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// Services
import { ContractService } from '../../../shared/services/contract.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingService } from '../../../core/services/loading.service';
import { AuthService } from '../../../auth/services/auth.service';

// Models
import { ContractResponse, FlatResponse } from '../../../shared/models';

// Components
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

// Pipes
import { CurrencyFormatPipe } from '../../../shared/pipes/currency-format.pipe';
import { PhoneFormatPipe } from '../../../shared/pipes/phone-format.pipe';

@Component({
  selector: 'app-contract-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    ButtonComponent,
    EmptyStateComponent,
    CurrencyFormatPipe,
    PhoneFormatPipe
  ],
  templateUrl: './contract-details.component.html',
  styleUrl: './contract-details.component.scss'
})
export class ContractDetailsComponent implements OnInit, OnDestroy {
  // Contract data
  contract: ContractResponse | null = null;
  flat: FlatResponse | null = null;
  
  // UI state
  isLoading = true;
  hasError = false;
  canManageContracts = false;
  
  // Contract status flags
  isActive = false;
  isPending = false;
  isExpired = false;
  isCancelled = false;
  isSuperseded = false;
  isRenewed = false;
  
  // Action availability
  canRenew = false;
  canCancel = false;
  canModify = false;
  
  private destroy$ = new Subject<void>();
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private contractService: ContractService,
    private flatService: FlatService,
    private notificationService: NotificationService,
    public loadingService: LoadingService,
    private authService: AuthService
  ) {}
  
  ngOnInit(): void {
    this.checkUserPermissions();
    this.loadContractDetails();
  }
  
  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
  
  /**
   * Check user permissions for contract management
   */
  private checkUserPermissions(): void {
    const user = this.authService.currentUserValue;
    if (user) {
      const hasAdminRole = user.roles?.some((role: string) => role === 'ROLE_ADMIN');
      const hasManagerRole = user.roles?.some((role: string) => role === 'ROLE_MANAGER');
      this.canManageContracts = hasAdminRole || hasManagerRole || false;
    }
  }
  
  /**
   * Load contract details from the API
   */
  private loadContractDetails(): void {
    const contractId = this.route.snapshot.paramMap.get('id');
    if (!contractId) {
      this.notificationService.error('Invalid contract ID');
      this.router.navigate(['/dashboard/contracts']);
      return;
    }
    
    this.isLoading = true;
    this.hasError = false;
    
    this.contractService.getContractById(Number(contractId))
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (contract) => {
          this.contract = contract;
          this.updateStatusFlags();
          this.updateActionFlags();
          // Skip loading flat details since we don't have buildingId in contract response
          // and the contract already contains basic flat info (flatNumber)
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading contract:', error);
          this.hasError = true;
          this.isLoading = false;
          this.notificationService.error('Failed to load contract details');
        }
      });
  }
  
  /**
   * Load flat details for the contract
   */
  private loadFlatDetails(): void {
    if (!this.contract || !this.contract.buildingId || !this.contract.flatId) {
      this.isLoading = false;
      return;
    }
    
    this.flatService.getFlat(this.contract.buildingId, this.contract.flatId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (flat) => {
          this.flat = flat;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Error loading flat:', error);
          // Not critical, continue without flat details
          this.isLoading = false;
        }
      });
  }
  
  /**
   * Update status flags based on contract status
   */
  private updateStatusFlags(): void {
    if (!this.contract) return;
    
    this.isActive = this.contract.status === 'ACTIVE';
    this.isPending = this.contract.status === 'PENDING';
    this.isExpired = this.contract.status === 'EXPIRED';
    this.isCancelled = this.contract.status === 'CANCELLED';
    this.isSuperseded = this.contract.status === 'SUPERSEDED';
    this.isRenewed = this.contract.status === 'RENEWED';
  }
  
  /**
   * Update action flags based on contract status and user permissions
   */
  private updateActionFlags(): void {
    if (!this.contract || !this.canManageContracts) {
      this.canRenew = false;
      this.canCancel = false;
      this.canModify = false;
      return;
    }
    
    // Only active contracts can be renewed if expiring soon
    this.canRenew = this.isActive && (this.contract.canRenew || this.contract.canBeRenewed || false);
    
    // Active or pending contracts can be cancelled
    this.canCancel = (this.isActive || this.isPending) && (this.contract.canBeCancelled || false);
    
    // Only active contracts can be modified if no dues generated yet
    this.canModify = this.isActive && (this.contract.canModify || this.contract.canBeModified || false);
  }
  
  /**
   * Navigate to contract renewal form
   */
  onRenewContract(): void {
    if (this.contract && this.canRenew) {
      this.router.navigate(['renew'], { relativeTo: this.route });
    }
  }
  
  /**
   * Navigate to contract cancellation dialog
   */
  onCancelContract(): void {
    if (this.contract && this.canCancel) {
      this.router.navigate(['cancel'], { relativeTo: this.route });
    }
  }
  
  /**
   * Navigate to contract modification form
   */
  onModifyContract(): void {
    if (this.contract && this.canModify) {
      this.router.navigate(['modify'], { relativeTo: this.route });
    }
  }
  
  /**
   * Navigate back to contracts list
   */
  onBack(): void {
    this.router.navigate(['/dashboard/contracts']);
  }
  
  /**
   * Get status badge CSS class
   */
  getStatusClass(): string {
    const statusClasses: { [key: string]: string } = {
      'ACTIVE': 'badge-success',
      'PENDING': 'badge-warning',
      'EXPIRED': 'badge-secondary',
      'CANCELLED': 'badge-danger',
      'RENEWED': 'badge-info',
      'SUPERSEDED': 'badge-secondary'
    };
    
    return statusClasses[this.contract?.status || ''] || 'badge-secondary';
  }
  
  /**
   * Get contract duration in months
   */
  get contractDuration(): number {
    if (!this.contract) return 0;
    
    const start = new Date(this.contract.startDate);
    const end = new Date(this.contract.endDate);
    
    const yearDiff = end.getFullYear() - start.getFullYear();
    const monthDiff = end.getMonth() - start.getMonth();
    const dayDiff = end.getDate() - start.getDate();
    
    let totalMonths = yearDiff * 12 + monthDiff;
    if (dayDiff > 0) {
      totalMonths++;
    }
    
    return Math.max(0, totalMonths);
  }
  
  /**
   * Format date for display
   */
  formatDate(date: string | Date): string {
    if (!date) return '-';
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
  
  /**
   * Get days until contract expires
   */
  get daysUntilExpiry(): number {
    if (!this.contract || !this.isActive) return 0;
    
    const today = new Date();
    const endDate = new Date(this.contract.endDate);
    const diffTime = endDate.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    return diffDays;
  }
  
  /**
   * Check if contract is expiring soon (within 30 days)
   */
  get isExpiringSoon(): boolean {
    return this.isActive && this.daysUntilExpiry > 0 && this.daysUntilExpiry <= 30;
  }
  
  /**
   * Get total contract value
   */
  get totalContractValue(): number {
    if (!this.contract) return 0;
    return this.contract.monthlyRent * this.contractDuration;
  }
  
  /**
   * Get payment due day with suffix (e.g., "1st", "2nd", "3rd", "4th")
   */
  get paymentDayWithSuffix(): string {
    if (!this.contract) return '-';
    
    const day = this.contract.dayOfMonth;
    const suffix = this.getDaySuffix(day);
    return `${day}${suffix}`;
  }
  
  /**
   * Get ordinal suffix for day
   */
  private getDaySuffix(day: number): string {
    if (day >= 11 && day <= 13) {
      return 'th';
    }
    
    switch (day % 10) {
      case 1: return 'st';
      case 2: return 'nd';
      case 3: return 'rd';
      default: return 'th';
    }
  }
}