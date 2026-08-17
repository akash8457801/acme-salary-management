import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { ApiService } from '../../core/api.service';
import { ApiError, EmployeeDetail } from '../../core/models';
import { toIsoDate } from './compensation-dialog.component';

export interface EmployeeFormDialogData {
  mode: 'create' | 'edit';
  employee?: EmployeeDetail;
}

/**
 * Hire someone, or edit an existing profile.
 *
 * In create mode the form asks for country and a starting salary — hiring is the one moment those
 * are set here. In edit mode both disappear: country moves are compensation events, and salary
 * changes go through the timeline dialog, where they are recorded with a reason.
 */
@Component({
  selector: 'app-employee-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
  ],
  templateUrl: './employee-form-dialog.component.html',
  styles: `
    form {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4px 16px;
      padding-top: 8px;
    }
    .full-width {
      grid-column: 1 / -1;
    }
    .server-error {
      grid-column: 1 / -1;
      color: #b71c1c;
      font-size: 13px;
      padding: 8px 0;
    }
  `,
})
export class EmployeeFormDialogComponent {
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<EmployeeFormDialogComponent>);
  private readonly formBuilder = inject(FormBuilder);

  readonly data: EmployeeFormDialogData = inject(MAT_DIALOG_DATA);
  readonly referenceData = toSignal(this.api.referenceData$);
  readonly serverError = signal<string | null>(null);
  readonly saving = signal(false);

  readonly isCreate = this.data.mode === 'create';

  readonly form = this.formBuilder.nonNullable.group({
    firstName: [this.data.employee?.firstName ?? '', [Validators.required, Validators.maxLength(80)]],
    lastName: [this.data.employee?.lastName ?? '', [Validators.required, Validators.maxLength(80)]],
    email: [this.data.employee?.email ?? '', [Validators.required, Validators.email]],
    departmentId: [this.data.employee?.departmentId ?? (null as number | null), Validators.required],
    countryCode: [{ value: this.data.employee?.countryCode ?? '', disabled: !this.isCreate },
      this.isCreate ? [Validators.required] : []],
    jobTitle: [this.data.employee?.jobTitle ?? '', [Validators.required, Validators.maxLength(120)]],
    level: [this.data.employee?.level ?? '', Validators.required],
    gender: [{ value: this.data.employee?.gender ?? '', disabled: !this.isCreate },
      this.isCreate ? [Validators.required] : []],
    status: [this.data.employee?.status ?? 'ACTIVE', Validators.required],
    hireDate: [{ value: null as Date | null, disabled: !this.isCreate },
      this.isCreate ? [Validators.required] : []],
    startingSalary: [{ value: null as number | null, disabled: !this.isCreate },
      this.isCreate ? [Validators.required, Validators.min(1)] : []],
  });

  /** The payroll currency implied by the selected country, shown next to the salary field. */
  get selectedCurrency(): string {
    const code = this.form.controls.countryCode.value;
    return this.referenceData()?.countries.find((c) => c.code === code)?.currency ?? '';
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.serverError.set(null);
    const value = this.form.getRawValue();

    const request$ = this.isCreate
      ? this.api.createEmployee({
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          departmentId: value.departmentId!,
          countryCode: value.countryCode,
          jobTitle: value.jobTitle,
          level: value.level,
          gender: value.gender,
          hireDate: toIsoDate(value.hireDate!),
          startingSalary: value.startingSalary!,
        })
      : this.api.updateEmployee(this.data.employee!.id, {
          firstName: value.firstName,
          lastName: value.lastName,
          email: value.email,
          departmentId: value.departmentId!,
          jobTitle: value.jobTitle,
          level: value.level,
          managerId: this.data.employee!.managerId,
          status: value.status,
        });

    request$.subscribe({
      next: (employee) => this.dialogRef.close(employee),
      error: (response: HttpErrorResponse) => {
        this.saving.set(false);
        const error = response.error as ApiError | undefined;
        this.serverError.set(
          error?.fieldErrors
            ? Object.entries(error.fieldErrors)
                .map(([field, message]) => `${field}: ${message}`)
                .join('; ')
            : (error?.message ?? 'Could not save.'),
        );
      },
    });
  }
}
