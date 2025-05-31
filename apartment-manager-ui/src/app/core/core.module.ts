import { NgModule, Optional, SkipSelf } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HTTP_INTERCEPTORS } from '@angular/common/http';
import { HttpErrorInterceptor } from './interceptors/http-error.interceptor';
import { LoadingInterceptor } from './interceptors/loading.interceptor';

/**
 * Core module that provides singleton services and interceptors.
 * This module should only be imported once in the AppModule.
 * It contains services that should have a single instance throughout the application.
 */
@NgModule({
  imports: [CommonModule],
  providers: [
    // HTTP Interceptors - order matters!
    {
      provide: HTTP_INTERCEPTORS,
      useClass: LoadingInterceptor,
      multi: true
    },
    {
      provide: HTTP_INTERCEPTORS,
      useClass: HttpErrorInterceptor,
      multi: true
    }
    // Singleton services are provided here or use providedIn: 'root'
  ]
})
export class CoreModule {
  /**
   * Constructor with singleton guard to prevent multiple imports
   * @param parentModule Parent module reference to check for existing import
   */
  constructor(@Optional() @SkipSelf() parentModule: CoreModule) {
    if (parentModule) {
      throw new Error(
        'CoreModule is already loaded. Import it in the AppModule only.'
      );
    }
  }
}