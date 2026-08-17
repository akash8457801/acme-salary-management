import { Pipe, PipeTransform } from '@angular/core';
import { Money } from './models';

/**
 * Formats a Money as the HR manager reads it: symbol, thousands grouping, no cents.
 *
 * <p>Cents are dropped deliberately — annual salaries are discussed in whole units everywhere,
 * and "€82,500.00" is noise in a 50-row table. The 'compact' mode ("$1.2M") is for chart axes
 * and stat cards where space is the constraint.
 */
@Pipe({ name: 'money', standalone: true })
export class MoneyPipe implements PipeTransform {
  transform(value: Money | null | undefined, mode: 'full' | 'compact' = 'full'): string {
    if (!value || value.amount === null || value.amount === undefined) {
      return '—';
    }
    const formatter = new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: value.currency,
      maximumFractionDigits: 0,
      ...(mode === 'compact' ? { notation: 'compact' as const, maximumFractionDigits: 1 } : {}),
    });
    return formatter.format(value.amount);
  }
}

/** Convenience for raw USD numbers coming out of the insights API. */
export function usd(amount: number | null | undefined): Money | null {
  return amount === null || amount === undefined ? null : { amount, currency: 'USD' };
}
