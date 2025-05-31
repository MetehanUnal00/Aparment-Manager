import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http'; // Import provideHttpClient, withFetch, and withInterceptors
import { authInterceptor } from './auth/interceptors/auth.interceptor';

import { routes } from './app-routing.module'; // Import the exported routes

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor]) // Add the auth interceptor
    )
    // If you were using BrowserAnimationsModule, you'd use provideAnimations() here
    // If you were using ServiceWorkerModule, you'd use provideServiceWorker() here
  ]
};
