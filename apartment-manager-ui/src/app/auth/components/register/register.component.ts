import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss'] // You can create this file for styling
})
export class RegisterComponent implements OnInit {
  form: any = {
    username: '',
    email: '',
    password: '',
    firstName: '',
    lastName: ''
  };
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    if (this.authService.isLoggedIn()) {
      // Optional: Redirect to dashboard if already logged in
      // this.router.navigate(['/dashboard']);
    }
  }

  onSubmit(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.authService.register(this.form).subscribe({
      next: data => {
        this.isLoading = false;
        this.successMessage = data.message + ' You can now login.';
        console.log('Registration successful', data);
        // Optionally redirect to login or show a success message
        // For now, we'll show a message and let the user click the login link
        // this.router.navigate(['/auth/login']);
      },
      error: err => {
        this.isLoading = false;
        if (err.error && typeof err.error === 'object' && err.error.message) {
          this.errorMessage = err.error.message;
        } else if (typeof err.error === 'string' && err.error.length > 0) {
          this.errorMessage = err.error;
        } else if (err.message) {
          this.errorMessage = err.message;
        } else {
          this.errorMessage = 'Registration failed. Please try again later.';
        }
        console.error('Registration error', err);
      }
    });
  }
}
