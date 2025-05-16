import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
// Import child components here if you have them, e.g.:
// import { OverviewComponent } from './overview/overview.component';
// import { ProfileComponent } from './profile/profile.component';

const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    // children: [ // Example child routes
    //   { path: '', redirectTo: 'overview', pathMatch: 'full' },
    //   { path: 'overview', component: OverviewComponent },
    //   { path: 'profile', component: ProfileComponent },
    // ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { }