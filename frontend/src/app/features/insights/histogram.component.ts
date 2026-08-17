import { CommonModule } from '@angular/common';
import { Component, Input, computed, signal } from '@angular/core';
import { SalaryBand } from '../../core/models';

/** Vertical histogram of the salary distribution, in plain SVG (ADR-007). */
@Component({
  selector: 'app-histogram',
  standalone: true,
  imports: [CommonModule],
  template: `
    <svg [attr.viewBox]="'0 0 ' + width + ' ' + height" preserveAspectRatio="xMidYMid meet">
      @for (bar of bars(); track bar.index) {
        <g>
          <rect
            [attr.x]="bar.x"
            [attr.y]="bar.y"
            [attr.width]="barWidth"
            [attr.height]="bar.barHeight"
            rx="3"
            fill="#3949ab"
          >
            <title>{{ bar.tooltip }}</title>
          </rect>
          <text [attr.x]="bar.x + barWidth / 2" [attr.y]="bar.y - 5" class="count">
            {{ bar.headcount }}
          </text>
          <text [attr.x]="bar.x + barWidth / 2" [attr.y]="height - 6" class="axis">
            {{ bar.axisLabel }}
          </text>
        </g>
      }
    </svg>
  `,
  styles: `
    svg {
      width: 100%;
      display: block;
    }
    .count {
      font-size: 10px;
      fill: #555;
      text-anchor: middle;
      font-variant-numeric: tabular-nums;
    }
    .axis {
      font-size: 9px;
      fill: #888;
      text-anchor: middle;
    }
  `,
})
export class HistogramComponent {
  readonly width = 640;
  readonly height = 190;
  readonly barWidth = 36;

  private readonly data = signal<SalaryBand[]>([]);

  @Input({ required: true }) set bandsInput(value: SalaryBand[]) {
    this.data.set(value ?? []);
  }

  readonly bars = computed(() => {
    const bands = this.data();
    const max = Math.max(...bands.map((band) => band.headcount), 1);
    const gap = bands.length > 1
      ? (this.width - bands.length * this.barWidth) / (bands.length - 1)
      : 0;
    const plotHeight = this.height - 40;

    return bands.map((band, index) => {
      const barHeight = Math.max((band.headcount / max) * plotHeight, 2);
      return {
        index,
        headcount: band.headcount,
        x: index * (this.barWidth + gap),
        y: this.height - 22 - barHeight,
        barHeight,
        axisLabel: this.compact(band.lowerBoundUsd) + (band.upperBoundUsd === null ? '+' : ''),
        tooltip:
          band.upperBoundUsd === null
            ? `${band.headcount} people earn over ${this.compact(band.lowerBoundUsd)}`
            : `${band.headcount} people earn ${this.compact(band.lowerBoundUsd)}–${this.compact(band.upperBoundUsd)}`,
      };
    });
  });

  private compact(usdAmount: number): string {
    return usdAmount >= 1000 ? `$${usdAmount / 1000}k` : `$${usdAmount}`;
  }
}
