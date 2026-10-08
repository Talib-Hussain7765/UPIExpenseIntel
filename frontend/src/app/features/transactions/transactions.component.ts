import { DatePipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../../shared/api-error';
import { Category } from '../categories/category.models';
import { CategoryService } from '../categories/category.service';
import { PaymentMode, Transaction, TransactionType } from './transaction.models';
import { TransactionService } from './transaction.service';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './transactions.component.html',
  styleUrl: './transactions.component.scss'
})
export class TransactionsComponent implements OnInit {
  readonly transactionTypes: TransactionType[] = ['DEBIT', 'CREDIT'];
  readonly paymentModes: PaymentMode[] = ['UPI', 'NEFT', 'IMPS', 'RTGS', 'ATM', 'POS', 'CARD', 'CHEQUE', 'INTEREST', 'CHARGES', 'UNKNOWN'];
  readonly categories = signal<Category[]>([]);
  readonly transactions = signal<Transaction[]>([]);
  readonly loading = signal(false);
  readonly savingId = signal<string | null>(null);
  readonly categorizing = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly filters = this.formBuilder.nonNullable.group({
    month: [this.currentMonth()],
    transactionType: [''],
    paymentMode: [''],
    categoryId: [''],
    merchant: [''],
    ignored: [''],
    transfer: [''],
    overwrite: [false]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly categoryService: CategoryService,
    private readonly transactionService: TransactionService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
    this.loadTransactions();
  }

  loadCategories(): void {
    this.categoryService.list().subscribe({
      next: (categories) => this.categories.set(categories),
      error: (error) => this.errorMessage.set(apiErrorMessage(error))
    });
  }

  loadTransactions(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    const values = this.filters.getRawValue();
    this.transactionService.list({
      month: values.month,
      transactionType: values.transactionType as TransactionType | '',
      paymentMode: values.paymentMode as PaymentMode | '',
      categoryId: this.trimToUndefined(values.categoryId),
      merchant: this.trimToUndefined(values.merchant),
      ignored: this.toBooleanFilter(values.ignored),
      transfer: this.toBooleanFilter(values.transfer)
    }).subscribe({
      next: (transactions) => {
        this.transactions.set(transactions);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  updateCategory(transaction: Transaction, categoryId: string): void {
    this.updateTransaction(transaction, { categoryId: categoryId || null });
  }

  toggleIgnored(transaction: Transaction): void {
    this.updateTransaction(transaction, { ignored: !transaction.ignored });
  }

  toggleTransfer(transaction: Transaction): void {
    this.updateTransaction(transaction, { transfer: !transaction.transfer });
  }

  rerunCategorization(): void {
    if (this.categorizing()) {
      return;
    }

    const values = this.filters.getRawValue();
    this.categorizing.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    console.debug('Re-run categorization requested', {
      month: values.month,
      overwrite: values.overwrite
    });
    this.transactionService.categorize(values.month, values.overwrite).subscribe({
      next: (response) => {
        this.successMessage.set(`Categorized ${response.updatedTransactions} transactions`);
        this.categorizing.set(false);
        this.loadTransactions();
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.categorizing.set(false);
      }
    });
  }

  resetFilters(): void {
    this.filters.reset({
      month: this.currentMonth(),
      transactionType: '',
      paymentMode: '',
      categoryId: '',
      merchant: '',
      ignored: '',
      transfer: '',
      overwrite: false
    });
    this.loadTransactions();
  }

  money(value: number): string {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0
    }).format(value);
  }

  private updateTransaction(transaction: Transaction, request: { categoryId?: string | null; transfer?: boolean; ignored?: boolean }): void {
    if (this.savingId()) {
      return;
    }

    this.savingId.set(transaction.id);
    this.errorMessage.set(null);
    this.transactionService.update(transaction.id, request).subscribe({
      next: (updated) => {
        this.transactions.update((items) => items.map((item) => item.id === updated.id ? updated : item));
        this.savingId.set(null);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.savingId.set(null);
      }
    });
  }

  private currentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  private trimToUndefined(value: string): string | undefined {
    const trimmed = value.trim();
    return trimmed ? trimmed : undefined;
  }

  private toBooleanFilter(value: string): boolean | '' {
    if (value === 'true') {
      return true;
    }
    if (value === 'false') {
      return false;
    }
    return '';
  }
}
