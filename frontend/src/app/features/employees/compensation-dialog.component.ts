import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatNativeDateModule } from '@angular/material/core';
import { MatSelectModule } from '@angular/material/select';
import { toSignal } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { ApiService } from '../../core/api.service';
import { ApiError, EmployeeDetail } from '../../core/models';
import { MoneyPipe } from '../../core/money.pipe';

/**
 * Recording a raise, promotion or correction.
 *
 * The currency is shown but not editable: the employee is paid in their country's payroll
 * currency, full stop. The live preview (change vs current salary) is decoration — the number
 * that counts is computed server-side, where it is tested.
 */
@Component({
  selector: 'app-compensation-dialog',
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
    MoneyPipe,
  ],
  templateUrl: './compensation-dialog.component.html',
  styles: `
    form {
      display: flex;
      flex-direction: column;
      gap: 4px;
      padding-top: 8px;
    }
    .current-line {
      font-size: 14px;
      color: #555;
      margin: 0 0 12px;
    }
    .preview {
      font-size: 14px;
      padding: 10px 14px;
      border-radius: 8px;
      background: #f1f3f9;
      margin-bottom: 8px;

      &.up { background: #e8f5e9; color: #1b5e20; }
      &.down { background: #fff8e1; color: #8d6e00; }
    }
    .server-error {
      color: #b71c1c;
      font-size: 13px;
      padding: 8px 0;
    }
  `,
})
export class CompensationDialogComponent {
  private readonly api = inject(ApiService);
  private readonly dialogRef = inject(MatDialogRef<CompensationDialogComponent>);
  private readonly formBuilder = inject(FormBuilder);

  readonly data: { employee: EmployeeDetail } = inject(MAT_DIALOG_DATA);
  readonly referenceData = toSignal(this.api.referenceData$);
  readonly serverError = signal<string | null>(null);
  readonly saving = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    effectiveFrom: [null as Date | null, Validators.required],
    reason: ['MERIT_INCREASE', Validators.required],
    note: [''],
  });

  get currency(): string {
    return this.data.employee.payrollCurrency;
  }

  /** Live % change against the current salary, for the preview line only. */
  get previewChangePercent(): number | null {
    const current = this.data.employee.currentCompensation?.salary.amount;
    const amount = this.form.controls.amount.value;
    if (!current || !amount) {
      return null;
    }
    return Math.round(((amount - current) / current) * 1000) / 10;
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saving.set(true);
    this.serverError.set(null);

    this.api
      .recordCompensationChange(this.data.employee.id, {
        amount: value.amount!,
        currency: this.currency,
        effectiveFrom: toIsoDate(value.effectiveFrom!),
        reason: value.reason,
        note: value.note || undefined,
      })
      .subscribe({
        next: (entry) => this.dialogRef.close(entry),
        error: (response: HttpErrorResponse) => {
          this.saving.set(false);
          const error = response.error as ApiError | undefined;
          this.serverError.set(error?.message ?? 'The change could not be saved.');
        },
      });
  }
}

/** Date -> yyyy-MM-dd without timezone drift (toISOString would shift across midnight UTC). */
export function toIsoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}
