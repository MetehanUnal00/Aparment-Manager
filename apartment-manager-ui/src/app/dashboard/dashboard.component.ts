import { Component, OnInit } from '@angular/core';
import { AuthService } from '../auth/services/auth.service';
import { Router, RouterLink, RouterOutlet, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ApartmentBuildingService } from '../shared/services/apartment-building.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    RouterOutlet,
    RouterLinkActive
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  currentUser: any;
  buildings: any[] = [];
  loadingError: string | null = null;

  constructor(
    public authService: AuthService, 
    private router: Router,
    private buildingService: ApartmentBuildingService
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.currentUserValue;
    if (!this.currentUser) {
      this.router.navigate(['/auth/login']);
    } else {
      // Test the API connection with JWT token
      this.testApiConnection();
    }
  }

  /**
   * Test the API connection by fetching buildings
   * This will verify that the JWT token is being sent correctly
   */
  testApiConnection(): void {
    console.log('Testing API connection with JWT token...');
    console.log('Current user:', this.currentUser);
    console.log('Token:', this.authService.getToken());
    
    this.buildingService.getAllBuildings().subscribe({
      next: (buildings) => {
        console.log('Successfully fetched buildings:', buildings);
        this.buildings = buildings;
        this.loadingError = null;
      },
      error: (error) => {
        console.error('Failed to fetch buildings:', error);
        this.loadingError = `Failed to load buildings: ${error.status} ${error.statusText}`;
      }
    });
  }

  logout(): void {
    this.authService.logout();
  }
}