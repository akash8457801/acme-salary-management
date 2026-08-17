import { toIsoDate } from './compensation-dialog.component';

describe('toIsoDate', () => {
  it('formats a local date without timezone drift', () => {
    // toISOString() on this date would give the previous day in any UTC+ timezone.
    expect(toIsoDate(new Date(2026, 0, 1))).toBe('2026-01-01');
    expect(toIsoDate(new Date(2026, 11, 31))).toBe('2026-12-31');
  });

  it('zero-pads single-digit months and days', () => {
    expect(toIsoDate(new Date(2026, 2, 5))).toBe('2026-03-05');
  });
});
