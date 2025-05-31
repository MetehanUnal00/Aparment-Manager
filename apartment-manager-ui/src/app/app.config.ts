import { ApplicationConfig, ErrorHandler, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'; // Import provideHttpClient, withFetch, and withInterceptors
import { authInterceptor } from './auth/interceptors/auth.interceptor';
import { loadingInterceptor } from './core/interceptors/loading.interceptor.func';
import { httpErrorInterceptor } from './core/interceptors/http-error.interceptor.func';
import { GlobalErrorHandler } from './core/services/error-handler.service';

import { routes } from './app-routing.module'; // Import the exported routes

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([
        loadingInterceptor,   // Loading interceptor first
        authInterceptor,      // Auth interceptor second
        httpErrorInterceptor  // Error interceptor last
      ])
    ),
    // Global error handler
    { provide: ErrorHandler, useClass: GlobalErrorHandler }
    // If you were using BrowserAnimationsModule, you'd use provideAnimations() here
    // If you were using ServiceWorkerModule, you'd use provideServiceWorker() here
  ]
};
