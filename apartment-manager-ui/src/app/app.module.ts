import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
// HttpClientModule should be provided via app.config.ts with provideHttpClient()
// AppRoutingModule is used to get the 'routes' for provideRouter in app.config.ts
import { AppComponent } from './app.component';
// AuthModule is lazy-loaded via routing, so it doesn't need to be in imports here for standalone bootstrap

@NgModule({
  declarations: [
    // AppComponent is standalone, so it's not declared here.
  ],
  imports: [
    BrowserModule,
    // AppRoutingModule, // Routes are provided via provideRouter in app.config.ts
    // HttpClientModule, // Provided via provideHttpClient in app.config.ts
    // AuthModule, // Lazy loaded
    AppComponent // Standalone components are imported if used by templates in this module (if AppModule was still primary)
  ],
  providers: [
    // provideClientHydration()
  ],
  // bootstrap: [AppComponent] // Not needed when using bootstrapApplication in main.ts
})
export class AppModule { }