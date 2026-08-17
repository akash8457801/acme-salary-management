import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'employees' },
  {
    path: 'employees',
    loadComponent: () =>
      import('./features/employees/employee-list.component').then((m) => m.EmployeeListComponent),
  },
  {
    path: 'employees/:id',
    loadComponent: () =>
      import('./features/employees/employee-detail.component').then(
        (m) => m.EmployeeDetailComponent,
      ),
  },
  {
    path: 'insights',
    loadComponent: () =>
      import('./features/insights/insights-dashboard.component').then(
        (m) => m.InsightsDashboardComponent,
      ),
  },
  { path: '**', redirectTo: 'employees' },
];
