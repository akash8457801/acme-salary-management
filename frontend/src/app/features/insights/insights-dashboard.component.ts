import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../core/api.service';
import { BreakdownRow, InsightsDashboard } from '../../core/models';
import { MoneyPipe, usd } from '../../core/money.pipe';
import { BarChartComponent, BarDatum } from './bar-chart.component';
import { HistogramComponent } from './histogram.component';

/**
 * "How does ACME pay people?" on one screen: headline numbers, cost by department and country,
 * pay by level, the distribution's shape, and the within-level gender pay gap.
 */
@Component({
  selector: 'app-insights-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
    MoneyPipe,
    BarChartComponent,
    HistogramComponent,
  ],
  templateUrl: './insights-dashboard.component.html',
  styleUrl: './insights-dashboard.component.scss',
})
export class InsightsDashboardComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly dashboard = signal<InsightsDashboard | null>(null);
  readonly loading = signal(true);

  readonly usd = usd;
  readonly equityColumns = ['level', 'women', 'men', 'gap'];

  ngOnInit(): void {
    this.api.insightsDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard.set(dashboard);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  costBars(rows: BreakdownRow[]): BarDatum[] {
    return rows.map((row) => ({
      label: row.label,
      value: row.totalAnnualUsd,
      detail: `${row.headcount.toLocaleString()} people`,
    }));
  }

  medianBars(rows: BreakdownRow[]): BarDatum[] {
    return rows.map((row) => ({
      label: row.label,
      value: row.medianAnnualUsd,
      detail: `p25 ${this.compact(row.p25AnnualUsd)} · p75 ${this.compact(row.p75AnnualUsd)}`,
    }));
  }

  readonly formatUsdCompact = (value: number): string => this.compact(value);

  private compact(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }
}
