import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
// HttpClientModule is not strictly needed here if provideHttpClient is in app.config.ts
// and services are 'providedIn: root' or provided elsewhere appropriately.
// However, having it won't hurt if components in this module were to directly import it,
// but the global provision is preferred for standalone apps.

import { AuthRoutingModule } from './auth-routing.module';
import { LoginComponent } from './components/login/login.component';
import { RegisterComponent } from './components/register/register.component';

@NgModule({
  declarations: [
    // Standalone components are not declared in NgModules.
  ],
  imports: [
    CommonModule,
    AuthRoutingModule,
    FormsModule,
    LoginComponent,    // Import standalone component
    RegisterComponent  // Import standalone component
  ]
})
export class AuthModule { }
