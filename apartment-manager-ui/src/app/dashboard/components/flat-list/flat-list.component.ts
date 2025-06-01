import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink, Router, ActivatedRoute } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { FlatService } from '../../../shared/services/flat.service';
import { NotificationService } from '../../../core/services/notification.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';
import { FlatResponse } from '../../../shared/models/flat.model';

/**
 * Component for displaying and managing flats within apartment buildings
 * Shows list of flats with tenant information, balance, and allows CRUD operations
 */
@Component({
  selector: 'app-flat-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    SearchBoxComponent,
    ButtonComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './flat-list.component.html',
  styleUrls: ['./flat-list.component.scss']
})
export class FlatListComponent implements OnInit, OnDestroy {
  // Inject services using the inject() function
  private readonly buildingService = inject(ApartmentBuildingService);
  private readonly flatService = inject(FlatService);
  private readonly notification = inject(NotificationService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  /**
   * Subject for component destruction
   */
  private readonly destroy$ = new Subject<void>();

  /**
   * Loading state indicator
   */
  loading = true;

  /**
   * Auto-refresh enabled state
   */
  autoRefreshEnabled = false;

  /**
   * List of buildings for selection
   */
  buildings: ApartmentBuildingResponse[] = [];

  /**
   * Currently selected building ID
   */
  selectedBuildingId: number | null = null;

  /**
   * Currently selected building details
   */
  selectedBuilding: ApartmentBuildingResponse | null = null;

  /**
   * List of flats in the selected building
   */
  flats: FlatResponse[] = [];

  /**
   * Filtered list of flats based on search
   */
  filteredFlats: FlatResponse[] = [];

  /**
   * Search term for filtering flats
   */
  searchTerm = '';

  /**
   * Sort column name
   */
  sortColumn: keyof FlatResponse = 'flatNumber';

  /**
   * Sort direction
   */
  sortDirection: 'asc' | 'desc' = 'asc';

  /**
   * Show only active flats
   */
  showActiveOnly = true;

  /**
   * Delete confirmation dialog state
   */
  showDeleteConfirm = false;
  flatToDelete: FlatResponse | null = null;

  ngOnInit(): void {
    // Check if building ID is passed in route params
    this.route.queryParams.pipe(takeUntil(this.destroy$)).subscribe(params => {
      if (params['buildingId']) {
        this.selectedBuildingId = +params['buildingId'];
      }
      this.loadBuildings();
    });
  }

  ngOnDestroy(): void {
    // Stop polling if active
    if (this.selectedBuildingId) {
      this.flatService.stopPolling(this.selectedBuildingId);
    }
    
    // Clean up subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Toggle auto-refresh functionality
   */
  toggleAutoRefresh(): void {
    this.autoRefreshEnabled = !this.autoRefreshEnabled;
    
    if (this.autoRefreshEnabled) {
      this.notification.info('Auto-refresh enabled (every 30 seconds)');
      this.loadFlats(true);
    } else {
      this.notification.info('Auto-refresh disabled');
      if (this.selectedBuildingId) {
        this.flatService.stopPolling(this.selectedBuildingId);
      }
      this.loadFlats(); // Reload without polling
    }
  }

  /**
   * Load all buildings
   */
  private loadBuildings(): void {
    this.loading = true;

    this.buildingService.getBuildings().subscribe({
      next: (buildings) => {
        this.buildings = buildings;
        
        // If building ID was passed in route, use it
        if (this.selectedBuildingId) {
          this.selectedBuilding = buildings.find(b => b.id === this.selectedBuildingId) || null;
          this.loadFlats();
        } else if (buildings.length > 0) {
          // Otherwise select first building by default
          this.selectedBuildingId = buildings[0].id;
          this.selectedBuilding = buildings[0];
          this.loadFlats();
        } else {
          this.loading = false;
        }
      },
      error: (error) => {
        this.notification.error('Failed to load buildings. Please try again.');
        this.loading = false;
        console.error('Error loading buildings:', error);
      }
    });
  }

  /**
   * Load flats for the selected building
   */
  private loadFlats(forceRefresh = false): void {
    if (!this.selectedBuildingId) return;

    if (!forceRefresh) {
      this.loading = true;
    }

    this.flatService.getFlatsByBuilding(this.selectedBuildingId, {
      forceRefresh,
      enablePolling: this.autoRefreshEnabled
    }).pipe(takeUntil(this.destroy$)).subscribe({
      next: (flats) => {
        this.flats = flats;
        this.filterFlats();
        this.loading = false;
      },
      error: (error) => {
        this.notification.error('Failed to load flats. Please try again.');
        this.loading = false;
        console.error('Error loading flats:', error);
      }
    });
  }

  /**
   * Handle building selection change
   */
  onBuildingChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    const buildingId = parseInt(target.value, 10);
    if (!isNaN(buildingId)) {
      // Stop polling for previous building
      if (this.selectedBuildingId) {
        this.flatService.stopPolling(this.selectedBuildingId);
      }
      
      this.selectedBuildingId = buildingId;
      this.selectedBuilding = this.buildings.find(b => b.id === buildingId) || null;
      this.loadFlats();
    }
  }

  /**
   * Filter flats based on search term and active status
   */
  filterFlats(): void {
    let filtered = this.flats;

    // Filter by active status
    if (this.showActiveOnly) {
      filtered = filtered.filter(flat => flat.isActive);
    }

    // Filter by search term
    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      filtered = filtered.filter(flat =>
        flat.flatNumber.toLowerCase().includes(term) ||
        (flat.tenantName && flat.tenantName.toLowerCase().includes(term)) ||
        (flat.tenantEmail && flat.tenantEmail.toLowerCase().includes(term))
      );
    }

    this.filteredFlats = filtered;
    this.sortFlats();
  }

  /**
   * Sort flats by specified column
   */
  sort(column: keyof FlatResponse): void {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.sortFlats();
  }

  /**
   * Perform the actual sorting
   */
  private sortFlats(): void {
    this.filteredFlats.sort((a, b) => {
      let aValue = a[this.sortColumn];
      let bValue = b[this.sortColumn];

      // Handle null/undefined values
      if (aValue == null) aValue = '';
      if (bValue == null) bValue = '';

      // Convert to numbers for numeric columns
      if (typeof aValue === 'number' && typeof bValue === 'number') {
        return this.sortDirection === 'asc' ? aValue - bValue : bValue - aValue;
      }

      // String comparison for other columns
      const comparison = String(aValue).localeCompare(String(bValue));
      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
  }

  /**
   * Handle search term change
   */
  onSearchChange(searchTerm: string): void {
    this.searchTerm = searchTerm;
    this.filterFlats();
  }

  /**
   * Toggle active only filter
   */
  toggleActiveFilter(): void {
    this.showActiveOnly = !this.showActiveOnly;
    this.filterFlats();
  }

  /**
   * Navigate to create new flat
   */
  createFlat(): void {
    if (this.selectedBuildingId) {
      this.router.navigate(['/dashboard/flats/new'], { 
        queryParams: { buildingId: this.selectedBuildingId } 
      });
    }
  }

  /**
   * Navigate to edit flat
   */
  editFlat(flat: FlatResponse): void {
    this.router.navigate(['/dashboard/flats', flat.id, 'edit'], { 
      queryParams: { buildingId: this.selectedBuildingId } 
    });
  }

  /**
   * Navigate to view flat details
   */
  viewFlat(flat: FlatResponse): void {
    this.router.navigate(['/dashboard/flats', flat.id], { 
      queryParams: { buildingId: this.selectedBuildingId } 
    });
  }

  /**
   * Show delete confirmation
   */
  confirmDelete(flat: FlatResponse): void {
    this.flatToDelete = flat;
    this.showDeleteConfirm = true;
  }

  /**
   * Handle delete confirmation
   */
  onDeleteConfirm(confirmed: boolean): void {
    if (confirmed && this.flatToDelete && this.selectedBuildingId) {
      this.flatService.deleteFlat(this.selectedBuildingId, this.flatToDelete.id).subscribe({
        next: () => {
          this.notification.success(`Flat "${this.flatToDelete!.flatNumber}" deactivated successfully`);
          this.loadFlats(true);
        },
        error: (error) => {
          this.notification.error('Failed to deactivate flat. Please try again.');
          console.error('Error deactivating flat:', error);
        }
      });
    }
    
    this.showDeleteConfirm = false;
    this.flatToDelete = null;
  }

  /**
   * Get count of active flats
   */
  getActiveFlatsCount(): number {
    return this.flats.filter(f => f.isActive).length;
  }

  /**
   * Get count of occupied flats
   */
  getOccupiedFlatsCount(): number {
    return this.flats.filter(f => !!f.tenantName).length;
  }

  /**
   * Manual refresh data
   */
  refreshData(): void {
    this.loadFlats(true);
    this.notification.info('Flats list refreshed');
  }

  /**
   * Navigate to add building page
   */
  navigateToAddBuilding = (): void => {
    this.router.navigate(['/dashboard/buildings/new']);
  }

  /**
   * Get occupancy status text
   */
  getOccupancyStatus(flat: FlatResponse): string {
    return flat.tenantName ? 'Occupied' : 'Vacant';
  }

  /**
   * Get occupancy badge class
   */
  getOccupancyBadgeClass(flat: FlatResponse): string {
    return flat.tenantName ? 'badge-success' : 'badge-secondary';
  }

  /**
   * Get balance class based on value
   */
  getBalanceClass(balance: number): string {
    if (balance > 0) return 'text-success';
    if (balance < 0) return 'text-danger';
    return '';
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

  /**
   * Format date values
   */
  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}