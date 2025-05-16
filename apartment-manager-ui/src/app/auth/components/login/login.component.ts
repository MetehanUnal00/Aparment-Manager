import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { FormsModule, NgForm } from '@angular/forms'; // Import FormsModule and NgForm
import { CommonModule } from '@angular/common'; // Import CommonModule for ngIf, ngClass etc.
import { ActivatedRoute, Router, RouterLink } from '@angular/router'; // Import ActivatedRoute

@Component({
  selector: 'app-login',
  standalone: true, // Ensure this is true if it's a standalone component
  imports: [
    CommonModule,    // Add CommonModule here
    FormsModule,     // Add FormsModule here
    RouterLink       // Add RouterLink for routerLink directive
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss']
})
export class LoginComponent implements OnInit {
  form: any = {
    username: '', // Initialize with empty strings
    password: ''  // Initialize with empty strings
  };
  isLoading = false;
  errorMessage = '';
  returnUrl: string = '';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute // Inject ActivatedRoute
  ) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']); // Or to the returnUrl if it exists
    }
    // Get the redirect URL from route parameters
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
  }

  onSubmit(): void { // No need to pass loginForm if using [(ngModel)] for form data
    if (!this.form.username || !this.form.password) {
        this.errorMessage = 'Username and password are required.';
        return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login({ username: this.form.username, password: this.form.password }).subscribe({
      next: data => {
        this.isLoading = false;
        console.log('Login successful', data);
        this.router.navigate([this.returnUrl]); // Navigate to the returnUrl
      },
      error: err => {
        this.isLoading = false;
        // Safely access the error message
        if (err.error && typeof err.error === 'object' && err.error.message) {
          this.errorMessage = err.error.message;
        } else if (typeof err.error === 'string' && err.error.length > 0) {
          // If err.error is a non-empty string, use it
          this.errorMessage = err.error;
        } else if (err.message) {
          // Fallback to err.message if available (e.g. network errors)
          this.errorMessage = err.message;
        } else {
          // Generic fallback
          this.errorMessage = 'Login failed. Please check your credentials or try again later.';
        }
        console.error('Login error', err);
      }
    });
  }
}
