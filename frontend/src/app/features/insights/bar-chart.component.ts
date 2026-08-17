import { CommonModule } from '@angular/common';
import { Component, Input, computed, signal } from '@angular/core';

export interface BarDatum {
  label: string;
  value: number;
  /** Extra line shown under the value, e.g. headcount. */
  detail?: string;
}

/**
 * A horizontal bar chart in plain SVG.
 *
 * Hand-rolled rather than a chart library (ADR-007): the dashboard needs exactly this shape, and
 * a hundred lines of SVG we own outright beats a megabyte of configurable dependency. Horizontal
 * because the labels are department and country names, which never fit under vertical bars.
 */
@Component({
  selector: 'app-bar-chart',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="chart">
      @for (bar of bars(); track bar.label) {
        <div class="row">
          <span class="label" [title]="bar.label">{{ bar.label }}</span>
          <svg [attr.height]="barHeight" width="100%" preserveAspectRatio="none">
            <rect
              x="0"
              y="2"
              [attr.width]="bar.percent + '%'"
              [attr.height]="barHeight - 4"
              rx="3"
              [attr.fill]="color"
            />
          </svg>
          <span class="value">
            {{ bar.formatted }}
            @if (bar.detail) {
              <span class="detail">{{ bar.detail }}</span>
            }
          </span>
        </div>
      }
    </div>
  `,
  styles: `
    .chart {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }
    .row {
      display: grid;
      grid-template-columns: 130px 1fr 120px;
      align-items: center;
      gap: 10px;
    }
    .label {
      font-size: 13px;
      color: #444;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
      text-align: right;
    }
    svg {
      display: block;
      background: #f4f5f9;
      border-radius: 3px;
    }
    .value {
      font-size: 13px;
      font-variant-numeric: tabular-nums;
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }
    .detail {
      color: #888;
      font-size: 11px;
    }
  `,
})
export class BarChartComponent {
  readonly barHeight = 22;

  @Input() color = '#3949ab';
  @Input() format: (value: number) => string = (value) => String(value);

  private readonly data = signal<BarDatum[]>([]);

  @Input({ required: true }) set items(value: BarDatum[]) {
    this.data.set(value ?? []);
  }

  readonly bars = computed(() => {
    const items = this.data();
    const max = Math.max(...items.map((item) => item.value), 1);
    return items.map((item) => ({
      ...item,
      percent: Math.max((item.value / max) * 100, 0.5),
      formatted: this.format(item.value),
    }));
  });
}
