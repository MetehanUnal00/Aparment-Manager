import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DashboardRoutingModule } from './dashboard-routing.module';
import { DashboardComponent } from './dashboard.component';

@NgModule({
  declarations: [
    // DashboardComponent is standalone, so it's not declared here.
  ],
  imports: [
    CommonModule,
    DashboardRoutingModule,
    DashboardComponent // Import the standalone DashboardComponent here
  ]
})
export class DashboardModule { }