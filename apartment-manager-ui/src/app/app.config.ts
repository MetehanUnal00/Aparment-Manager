import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http'; // Import provideHttpClient and withFetch

import { routes } from './app-routing.module'; // Import the exported routes

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withFetch()) // Add this to provide HttpClient
    // If you were using BrowserAnimationsModule, you'd use provideAnimations() here
    // If you were using ServiceWorkerModule, you'd use provideServiceWorker() here
  ]
};
