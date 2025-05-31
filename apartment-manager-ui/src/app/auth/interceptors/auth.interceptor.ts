import { inject } from '@angular/core';
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { Router } from '@angular/router';

/**
 * HTTP Interceptor function that automatically adds JWT token to outgoing requests
 * and handles authentication errors
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Inject services using the inject function
  const authService = inject(AuthService);
  const router = inject(Router);
  
  // Get the authentication token from the auth service
  const token = authService.getToken();
  
  // Clone the request and add the authorization header if token exists
  if (token) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  // Handle the request and catch any errors
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // If we get a 401 Unauthorized response, the token might be invalid or expired
      if (error.status === 401) {
        // Log the user out and redirect to login
        authService.logout();
        router.navigate(['/auth/login'], {
          queryParams: { returnUrl: router.url }
        });
      }
      
      // Re-throw the error so it can be handled by the calling component
      return throwError(() => error);
    })
  );
};