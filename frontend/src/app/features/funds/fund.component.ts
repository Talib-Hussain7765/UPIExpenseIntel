import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import {
  Fund,
  FundActivityPeriod,
  FundSummary,
  FundTransactionHistory
} from './fund.model';
import { FundService } from './fund.service';
import {
  FundTransactionDialogComponent,
  FundTransactionDialogResult
} from './fund-transaction-dialog.component';

@Component({
  selector: 'app-fund',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './fund.component.html',
  styleUrl: './fund.component.scss'
})
export class FundComponent implements OnInit {

  summary = signal<FundSummary | null>(null);

  loading = signal(false);
  activityLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  activity = signal<FundTransactionHistory | null>(null);
  selectedActivityFundId = signal<string | null>(null);

  newFundName = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required]
  });

  totalBalance = new FormControl<number | null>(null, {
    validators: [
      Validators.required,
      Validators.min(0)
    ]
  });

  reconciledDate = new FormControl('', {
    nonNullable: true,
    validators: [Validators.required]
  });

  activityPeriod = new FormControl<FundActivityPeriod>('LAST_MONTH', {
    nonNullable: true
  });

  activityStartDate = new FormControl('', {
    nonNullable: true
  });

  activityEndDate = new FormControl('', {
    nonNullable: true
  });

  constructor(
    private readonly fundService: FundService,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.reconciledDate.setValue(
      new Date().toISOString().substring(0, 10)
    );

    this.loadSummary();
  }

  loadSummary(): void {

    this.loading.set(true);
    this.errorMessage.set('');

    this.fundService.getSummary().subscribe({
      next: data => {
        this.summary.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.errorMessage.set(
          'Unable to load fund details.'
        );
        this.loading.set(false);
      }
    });
  }

  reconcile(): void {

    if (
      this.totalBalance.invalid ||
      this.reconciledDate.invalid
    ) {
      return;
    }

    const balance = this.totalBalance.value;

    if (balance === null) {
      return;
    }

    this.fundService.reconcile({
      totalBalance: balance,
      reconciledDate: this.reconciledDate.value
    }).subscribe({
      next: () => {
        this.successMessage.set(
          'Total balance updated successfully.'
        );

        this.totalBalance.reset();

        this.loadSummary();
      },
      error: () => {
        this.errorMessage.set(
          'Unable to update total balance.'
        );
      }
    });
  }

  createFund(): void {

    if (this.newFundName.invalid) {
      return;
    }

    const name = this.newFundName.value.trim();

    if (!name) {
      return;
    }

    this.fundService.createFund({
      name
    }).subscribe({
      next: () => {
        this.newFundName.reset();
        this.successMessage.set('Fund created.');
        this.loadSummary();
      },
      error: () => {
        this.errorMessage.set(
          'Unable to create fund.'
        );
      }
    });
  }

  changeFundBalance(
    fund: Fund,
    type: 'ADD' | 'WITHDRAW'
  ): void {
    this.dialog.open(FundTransactionDialogComponent, {
      width: '560px',
      maxWidth: 'calc(100vw - 32px)',
      data: {
        fundName: fund.name,
        type
      }
    }).afterClosed().subscribe((result: FundTransactionDialogResult | null) => {
      if (!result) {
        return;
      }

      if (type === 'WITHDRAW' && result.amount > fund.balance) {
        this.errorMessage.set('Withdrawal cannot exceed fund balance.');
        return;
      }

      this.fundService.updateFund(
        fund.id,
        {
          amount: result.amount,
          type,
          transactionDate: result.reconciledDate,
          note: result.note,
          reconciledTotalBalance: result.reconciledTotalBalance,
          reconciledDate: result.reconciledDate
        }
      ).subscribe({
        next: () => {
          this.successMessage.set(
            type === 'ADD'
              ? 'Amount added to fund and total balance reconciled.'
              : 'Amount withdrawn from fund and total balance reconciled.'
          );

          this.loadSummary();

          if (this.activity()) {
            this.loadActivity();
          }
        },
        error: () => {
          this.errorMessage.set('Unable to update fund.');
        }
      });
    });
  }

  selectActivityFund(fundId: string | null): void {
    this.selectedActivityFundId.set(fundId);
  }

  viewFundActivity(fund: Fund): void {
    this.selectedActivityFundId.set(fund.id);
    this.loadActivity();
  }

  loadActivity(): void {
    const period = this.activityPeriod.value;
    const startDate = this.activityStartDate.value || null;
    const endDate = this.activityEndDate.value || null;

    if (period === 'CUSTOM' && (!startDate || !endDate)) {
      this.errorMessage.set('Choose both dates for a custom activity period.');
      return;
    }

    if (startDate && endDate && endDate < startDate) {
      this.errorMessage.set('Activity end date must be on or after the start date.');
      return;
    }

    this.activityLoading.set(true);
    this.errorMessage.set('');

    this.fundService.getTransactions({
      fundId: this.selectedActivityFundId(),
      period,
      startDate,
      endDate
    }).subscribe({
      next: data => {
        this.activity.set(data);
        this.activityLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Unable to load fund activity.');
        this.activityLoading.set(false);
      }
    });
  }

  selectedActivityFundName(): string {
    const fundId = this.selectedActivityFundId();

    if (!fundId) {
      return 'All funds';
    }

    return this.summary()?.funds.find(fund => fund.id === fundId)?.name
      ?? 'Selected fund';
  }

  movementAmount(amount: number, type: 'ADD' | 'WITHDRAW'): string {
    const prefix = type === 'ADD' ? '+' : '-';
    return `${prefix}${this.money(amount)}`;
  }

  money(value: number): string {

    return new Intl.NumberFormat(
      'en-IN',
      {
        style: 'currency',
        currency: 'INR'
      }
    ).format(value);
  }
}
