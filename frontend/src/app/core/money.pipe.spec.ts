import { MoneyPipe, usd } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();

  it('formats a salary with symbol and grouping, no cents', () => {
    expect(pipe.transform({ amount: 82500, currency: 'USD' })).toBe('$82,500');
    expect(pipe.transform({ amount: 82500.75, currency: 'USD' })).toBe('$82,501');
  });

  it('uses the currency of the money, not a default', () => {
    expect(pipe.transform({ amount: 80000, currency: 'EUR' })).toBe('€80,000');
    expect(pipe.transform({ amount: 6500000, currency: 'INR' })).toBe('₹6,500,000');
  });

  it('renders a dash for missing values instead of pretending zero', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform(undefined)).toBe('—');
  });

  it('compacts large amounts for chart labels', () => {
    expect(pipe.transform({ amount: 1_234_000, currency: 'USD' }, 'compact')).toBe('$1.2M');
  });
});

describe('usd helper', () => {
  it('wraps a raw number as USD money', () => {
    expect(usd(500)).toEqual({ amount: 500, currency: 'USD' });
  });

  it('passes null through so the pipe can render a dash', () => {
    expect(usd(null)).toBeNull();
    expect(usd(undefined)).toBeNull();
  });
});
