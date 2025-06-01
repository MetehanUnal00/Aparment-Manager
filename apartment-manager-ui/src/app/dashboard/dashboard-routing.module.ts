import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DashboardComponent } from './dashboard.component';
import { DashboardOverviewComponent } from './components/dashboard-overview/dashboard-overview.component';
import { DebtorListComponent } from './components/debtor-list/debtor-list.component';
import { ApartmentBuildingListComponent } from './components/apartment-building-list/apartment-building-list.component';
import { ApartmentBuildingFormComponent } from './components/apartment-building-form/apartment-building-form.component';
import { FlatListComponent } from './components/flat-list/flat-list.component';
import { FlatFormComponent } from './components/flat-form/flat-form.component';
import { PaymentListComponent } from './components/payment-list/payment-list.component';
import { PaymentFormComponent } from './components/payment-form/payment-form.component';
import { ExpenseListComponent } from './components/expense-list/expense-list.component';
import { ExpenseFormComponent } from './components/expense-form/expense-form.component';
import { MonthlyDueListComponent } from './components/monthly-due-list/monthly-due-list.component';
import { MonthlyDueFormComponent } from './components/monthly-due-form/monthly-due-form.component';
import { ContractListComponent } from './components/contract-list/contract-list.component';

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
      { path: 'flats/:id/edit', component: FlatFormComponent },
      { path: 'payments', component: PaymentListComponent },
      { path: 'payments/new', component: PaymentFormComponent },
      { path: 'payments/:id/edit', component: PaymentFormComponent },
      { path: 'expenses', component: ExpenseListComponent },
      { path: 'expenses/new', component: ExpenseFormComponent },
      { path: 'expenses/:id/edit', component: ExpenseFormComponent },
      { path: 'monthly-dues', component: MonthlyDueListComponent },
      { path: 'monthly-dues/new', component: MonthlyDueFormComponent },
      { path: 'monthly-dues/:id/edit', component: MonthlyDueFormComponent },
      { path: 'contracts', component: ContractListComponent },
      { path: 'contracts/new', component: ContractListComponent }, // Placeholder until ContractFormComponent is ready
      { path: 'contracts/:id', component: ContractListComponent }, // Placeholder until ContractDetailsComponent is ready
      { path: 'contracts/:id/renew', component: ContractListComponent }, // Placeholder until ContractRenewalFormComponent is ready
      { path: 'contracts/:id/modify', component: ContractListComponent }, // Placeholder until ContractModificationFormComponent is ready
      { path: 'contracts/:id/cancel', component: ContractListComponent } // Placeholder until ContractCancellationDialogComponent is ready
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class DashboardRoutingModule { }