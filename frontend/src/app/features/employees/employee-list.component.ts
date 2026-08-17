import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatTableModule } from '@angular/material/table';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { EmployeeListQuery, EmployeeSummary, Page } from '../../core/models';
import { MoneyPipe } from '../../core/money.pipe';
import { EmployeeFormDialogComponent } from './employee-form-dialog.component';

/**
 * The directory: 10,000 employees, of which the browser only ever holds one page.
 *
 * Filter state lives in the URL, so "Engineering in Germany sorted by salary" is a link the HR
 * manager can bookmark or send to a colleague, and the back button walks back through views
 * rather than losing them.
 */
@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatDialogModule,
    MoneyPipe,
  ],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.scss',
})
export class EmployeeListComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly referenceData = toSignal(this.api.referenceData$);
  readonly result = signal<Page<EmployeeSummary> | null>(null);
  readonly loading = signal(false);

  readonly columns = [
    'employeeCode',
    'name',
    'department',
    'country',
    'jobTitle',
    'level',
    'salary',
    'annualUsd',
    'status',
  ];

  query: EmployeeListQuery = {};
  private readonly searchInput$ = new Subject<string>();

  ngOnInit(): void {
    this.searchInput$
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe((term) => this.patchQuery({ q: term || undefined, page: 0 }));

    // The URL is the single source of truth; every change re-queries the server.
    this.route.queryParams.subscribe((params) => {
      this.query = {
        q: params['q'] || undefined,
        departmentId: params['departmentId'] ? +params['departmentId'] : undefined,
        countryCode: params['countryCode'] || undefined,
        level: params['level'] || undefined,
        status: params['status'] || undefined,
        minAnnualUsd: params['minAnnualUsd'] ? +params['minAnnualUsd'] : undefined,
        maxAnnualUsd: params['maxAnnualUsd'] ? +params['maxAnnualUsd'] : undefined,
        sort: params['sort'] || 'NAME',
        direction: params['direction'] === 'DESC' ? 'DESC' : 'ASC',
        page: params['page'] ? +params['page'] : 0,
        size: params['size'] ? +params['size'] : 25,
      };
      this.fetch();
    });
  }

  onSearchTyped(term: string): void {
    this.searchInput$.next(term.trim());
  }

  onFilterChanged(): void {
    this.patchQuery({
      departmentId: this.query.departmentId,
      countryCode: this.query.countryCode,
      level: this.query.level,
      status: this.query.status,
      page: 0,
    });
  }

  onSortChanged(sort: Sort): void {
    this.patchQuery({
      sort: sort.direction ? this.sortKeyFor(sort.active) : undefined,
      direction: sort.direction === 'desc' ? 'DESC' : 'ASC',
      page: 0,
    });
  }

  onPageChanged(event: PageEvent): void {
    this.patchQuery({ page: event.pageIndex, size: event.pageSize });
  }

  clearFilters(): void {
    this.router.navigate([], { queryParams: {} });
  }

  get hasFilters(): boolean {
    const { q, departmentId, countryCode, level, status } = this.query;
    return !!(q || departmentId || countryCode || level || status);
  }

  exportCsv(): void {
    window.open(this.api.exportUrl({ ...this.query, page: undefined, size: undefined }), '_blank');
  }

  openHireDialog(): void {
    this.dialog
      .open(EmployeeFormDialogComponent, { width: '560px', data: { mode: 'create' } })
      .afterClosed()
      .subscribe((created) => {
        if (created) {
          this.snackBar.open(`${created.fullName} hired as ${created.jobTitle}`, 'View', {
            duration: 5000,
          });
          this.fetch();
        }
      });
  }

  trackById(_index: number, employee: EmployeeSummary): number {
    return employee.id;
  }

  private fetch(): void {
    this.loading.set(true);
    this.api.listEmployees(this.query).subscribe({
      next: (page) => {
        this.result.set(page);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Could not load employees — is the API running?', 'Dismiss');
      },
    });
  }

  private patchQuery(patch: Partial<EmployeeListQuery>): void {
    this.router.navigate([], {
      queryParams: { ...this.query, ...patch },
      queryParamsHandling: 'merge',
      replaceUrl: false,
    });
  }

  /** Material sort column ids → API sort keys. */
  private sortKeyFor(column: string): string {
    const mapping: Record<string, string> = {
      employeeCode: 'EMPLOYEE_CODE',
      name: 'NAME',
      department: 'DEPARTMENT',
      country: 'COUNTRY',
      level: 'LEVEL',
      salary: 'SALARY',
      annualUsd: 'SALARY',
      hireDate: 'HIRE_DATE',
    };
    return mapping[column] ?? 'NAME';
  }
}
