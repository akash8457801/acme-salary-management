import { CommonModule } from '@angular/common';
import { Component, Input, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { EmployeeDetail } from '../../core/models';
import { MoneyPipe } from '../../core/money.pipe';
import { CompensationDialogComponent } from './compensation-dialog.component';
import { EmployeeFormDialogComponent } from './employee-form-dialog.component';

/**
 * One person: who they are, what they earn, and every change we have ever made to it.
 * The timeline is the point of this page — the thing the spreadsheet could never show.
 */
@Component({
  selector: 'app-employee-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressBarModule,
    MatDialogModule,
    MatTooltipModule,
    MoneyPipe,
  ],
  templateUrl: './employee-detail.component.html',
  styleUrl: './employee-detail.component.scss',
})
export class EmployeeDetailComponent implements OnInit {
  /** Bound from the :id route parameter by withComponentInputBinding. */
  @Input({ required: true }) id!: string;

  private readonly api = inject(ApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly employee = signal<EmployeeDetail | null>(null);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.fetch();
  }

  openCompensationDialog(): void {
    const employee = this.employee();
    if (!employee) {
      return;
    }
    this.dialog
      .open(CompensationDialogComponent, { width: '480px', data: { employee } })
      .afterClosed()
      .subscribe((recorded) => {
        if (recorded) {
          this.snackBar.open('Salary change recorded', undefined, { duration: 4000 });
          this.fetch();
        }
      });
  }

  openEditDialog(): void {
    const employee = this.employee();
    if (!employee) {
      return;
    }
    this.dialog
      .open(EmployeeFormDialogComponent, { width: '560px', data: { mode: 'edit', employee } })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.snackBar.open('Profile updated', undefined, { duration: 4000 });
          this.fetch();
        }
      });
  }

  private fetch(): void {
    this.loading.set(true);
    this.api.employee(+this.id).subscribe({
      next: (detail) => {
        this.employee.set(detail);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Employee not found', 'Back to list');
      },
    });
  }
}
