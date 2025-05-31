import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

// Shared components
import { ButtonComponent } from '../../../shared/components/button/button.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';
import { SearchBoxComponent } from '../../../shared/components/search-box/search-box.component';
import { ConfirmDialogComponent } from '../../../shared/components/confirm-dialog/confirm-dialog.component';

// Services
import { ApartmentBuildingService } from '../../../shared/services/apartment-building.service';
import { NotificationService } from '../../../core/services/notification.service';

// Models
import { ApartmentBuildingResponse } from '../../../shared/models/apartment-building.model';

@Component({
  selector: 'app-apartment-building-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ButtonComponent,
    LoadingSpinnerComponent,
    EmptyStateComponent,
    SearchBoxComponent,
    ConfirmDialogComponent
  ],
  templateUrl: './apartment-building-list.component.html',
  styleUrls: ['./apartment-building-list.component.scss']
})
export class ApartmentBuildingListComponent implements OnInit, OnDestroy {
  // Service injections using the inject() pattern
  private apartmentBuildingService = inject(ApartmentBuildingService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  // Component state
  buildings: ApartmentBuildingResponse[] = [];
  filteredBuildings: ApartmentBuildingResponse[] = [];
  isLoading = false;
  searchTerm = '';
  
  // For confirmation dialog
  showDeleteConfirm = false;
  buildingToDelete: ApartmentBuildingResponse | null = null;

  // Cleanup subject for subscriptions
  private destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Load buildings on component initialization
    this.loadBuildings();
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all apartment buildings from the service
   */
  loadBuildings(): void {
    this.isLoading = true;
    
    this.apartmentBuildingService.getBuildings()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (buildings) => {
          this.buildings = buildings;
          this.filteredBuildings = buildings;
          this.isLoading = false;
          
          // Apply current search filter if any
          if (this.searchTerm) {
            this.filterBuildings(this.searchTerm);
          }
        },
        error: (error) => {
          this.isLoading = false;
          console.error('Error loading buildings:', error);
          // Error notification handled by service
        }
      });
  }

  /**
   * Filter buildings based on search term
   */
  filterBuildings(searchTerm: string): void {
    this.searchTerm = searchTerm;
    
    if (!searchTerm.trim()) {
      this.filteredBuildings = this.buildings;
      return;
    }

    const term = searchTerm.toLowerCase();
    this.filteredBuildings = this.buildings.filter(building =>
      building.name.toLowerCase().includes(term) ||
      (building.address && building.address.toLowerCase().includes(term))
    );
  }

  /**
   * Navigate to create new building form
   */
  createBuilding(): void {
    this.router.navigate(['/dashboard/buildings/new']);
  }

  /**
   * Navigate to edit building form
   */
  editBuilding(building: ApartmentBuildingResponse): void {
    this.router.navigate(['/dashboard/buildings', building.id, 'edit']);
  }

  /**
   * Show delete confirmation dialog
   */
  confirmDelete(building: ApartmentBuildingResponse): void {
    this.buildingToDelete = building;
    this.showDeleteConfirm = true;
  }

  /**
   * Handle delete confirmation
   */
  onDeleteConfirm(): void {
    if (!this.buildingToDelete) return;

    const buildingId = this.buildingToDelete.id;
    const buildingName = this.buildingToDelete.name;

    this.apartmentBuildingService.deleteBuilding(buildingId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.notificationService.success(`Building "${buildingName}" deleted successfully`);
          this.loadBuildings(); // Reload the list
          this.showDeleteConfirm = false;
          this.buildingToDelete = null;
        },
        error: (error) => {
          console.error('Error deleting building:', error);
          // Error notification handled by service
          this.showDeleteConfirm = false;
          this.buildingToDelete = null;
        }
      });
  }

  /**
   * Handle delete cancellation
   */
  onDeleteCancel(): void {
    this.showDeleteConfirm = false;
    this.buildingToDelete = null;
  }

  /**
   * Navigate to building details/flats
   */
  viewBuilding(building: ApartmentBuildingResponse): void {
    this.router.navigate(['/dashboard/buildings', building.id, 'flats']);
  }

}