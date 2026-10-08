import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { NgxEchartsModule } from 'ngx-echarts';
import type { EChartsOption } from 'echarts';
import { apiErrorMessage } from '../../shared/api-error';
import { DashboardDateBasis, DashboardSummaryResponse } from './dashboard.models';
import { DashboardService } from './dashboard.service';
import { FormControl, Validators } from '@angular/forms';
import { Fund } from '../funds/fund.model';
import { FundService } from '../funds/fund.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    NgxEchartsModule
  ],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent implements OnInit {
  readonly form = this.formBuilder.nonNullable.group({
    dateBasis: ['MONTH' as DashboardDateBasis],
    month: [this.currentMonth()],
    startDate: [''],
    endDate: ['']
  });
  readonly loading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly summary = signal<DashboardSummaryResponse | null>(null);
  readonly dailyTrendOptions = computed<EChartsOption>(() => this.toDailyTrendOptions(this.summary()));

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly dashboardService: DashboardService
  ) {}

  ngOnInit(): void {
    this.loadSummary();
    this.form.controls.dateBasis.valueChanges.subscribe((dateBasis) => {
      if (dateBasis !== 'CUSTOM_RANGE') {
        this.form.controls.startDate.setValue('');
        this.form.controls.endDate.setValue('');
      }
    });
  }

  loadSummary(): void {
    const value = this.form.getRawValue();
    if (value.dateBasis === 'CUSTOM_RANGE') {
      if (!value.startDate || !value.endDate) {
        this.errorMessage.set('Select both dashboard dates');
        return;
      }
      if (value.endDate < value.startDate) {
        this.errorMessage.set('End date must be on or after start date');
        return;
      }
    }

    this.loading.set(true);
    this.errorMessage.set(null);

    this.dashboardService.summary({
      dateBasis: value.dateBasis,
      month: value.month,
      startDate: value.dateBasis === 'CUSTOM_RANGE' ? value.startDate : null,
      endDate: value.dateBasis === 'CUSTOM_RANGE' ? value.endDate : null
    }).subscribe({
      next: (summary) => {
        this.summary.set(summary);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  money(value: number | null | undefined): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(value ?? 0);
  }

  percent(value: number | null | undefined): string {
    return `${(value ?? 0).toFixed(2)}%`;
  }

  customRangeSelected(): boolean {
    return this.form.controls.dateBasis.value === 'CUSTOM_RANGE';
  }

  private currentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  private toDailyTrendOptions(summary: DashboardSummaryResponse | null): EChartsOption {
    const trend = summary?.dailyExpenseTrend ?? [];
    return {
      color: ['#2563eb'],
      grid: { left: 48, right: 18, top: 24, bottom: 38 },
      tooltip: {
        trigger: 'axis',
        valueFormatter: (value) => this.money(Number(value))
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: trend.map((item) => this.dateAxisLabel(item.date))
      },
      yAxis: {
        type: 'value',
        axisLabel: {
          formatter: (value: number) => `₹${Math.round(value / 1000)}k`
        }
      },
      series: [
        {
          name: 'Expenses',
          type: 'line',
          smooth: true,
          areaStyle: { opacity: 0.12 },
          symbolSize: 5,
          data: trend.map((item) => item.amount)
        }
      ]
    };
  }

  private dateAxisLabel(value: string): string {
    return new Date(`${value}T00:00:00`).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short'
    });
  }
}
