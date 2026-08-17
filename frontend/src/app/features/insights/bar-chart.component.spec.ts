import { BarChartComponent } from './bar-chart.component';

describe('BarChartComponent', () => {
  function chartWith(values: number[]): BarChartComponent {
    const component = new BarChartComponent();
    component.items = values.map((value, index) => ({ label: `item ${index}`, value }));
    return component;
  }

  it('scales the longest bar to 100%', () => {
    const bars = chartWith([50, 100, 25]).bars();
    expect(bars[1].percent).toBe(100);
    expect(bars[0].percent).toBe(50);
    expect(bars[2].percent).toBe(25);
  });

  it('keeps zero-value bars faintly visible rather than invisible', () => {
    const bars = chartWith([100, 0]).bars();
    expect(bars[1].percent).toBeGreaterThan(0);
  });

  it('survives an empty dataset', () => {
    expect(chartWith([]).bars()).toEqual([]);
  });

  it('applies the provided formatter to values', () => {
    const component = chartWith([1500]);
    component.format = (value) => `$${value / 1000}k`;
    expect(component.bars()[0].formatted).toBe('$1.5k');
  });
});
