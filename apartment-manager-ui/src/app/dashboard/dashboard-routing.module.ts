import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { DashboardOverviewComponent } from './components/dashboard-overview/dashboard-overview.component';
import { DebtorListComponent } from './components/debtor-list/debtor-list.component';
import { ApartmentBuildingListComponent } from './components/apartment-building-list/apartment-building-list.component';
import { ApartmentBuildingFormComponent } from './components/apartment-building-form/apartment-building-form.component';
import { FlatListComponent } from './components/flat-list/flat-list.component';
import { FlatFormComponent } from './components/flat-form/flat-form.component';

const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    children: [
      { path: '', redirectTo: 'overview', pathMatch: 'full' },
      { path: 'overview', component: DashboardOverviewComponent },
      { path: 'debtors', component: DebtorListComponent },
      { path: 'buildings', component: ApartmentBuildingListComponent },
      { path: 'buildings/new', component: ApartmentBuildingFormComponent },
      { path: 'buildings/:id/edit', component: ApartmentBuildingFormComponent },
      { path: 'flats', component: FlatListComponent },
      { path: 'flats/new', component: FlatFormComponent },
      { path: 'flats/:id/edit', component: FlatFormComponent }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { }