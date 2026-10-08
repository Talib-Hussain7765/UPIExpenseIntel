import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { guestGuard } from './core/auth/guest.guard';
import { AppShellComponent } from './core/layout/app-shell/app-shell.component';
import { LoginComponent } from './features/auth/login/login.component';
import { BankAccountsComponent } from './features/bank-accounts/bank-accounts.component';
import { CategoriesComponent } from './features/categories/categories.component';
import { MerchantRulesComponent } from './features/merchant-rules/merchant-rules.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TransactionsComponent } from './features/transactions/transactions.component';
import { UploadsComponent } from './features/uploads/uploads.component';
import { FundComponent } from './features/funds/fund.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'bank-accounts', component: BankAccountsComponent },
      { path: 'uploads', component: UploadsComponent },
      { path: 'transactions', component: TransactionsComponent },
      { path: 'categories', component: CategoriesComponent },
      { path: 'merchant-rules', component: MerchantRulesComponent },
      { path: 'funds', component: FundComponent },
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' }
    ]
  },
  { path: '**', redirectTo: '' }
];
