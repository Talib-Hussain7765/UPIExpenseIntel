import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { apiErrorMessage } from '../../shared/api-error';
import { BankAccount } from '../bank-accounts/bank-account.models';
import { BankAccountService } from '../bank-accounts/bank-account.service';
import { StatementImportDateBasis, StatementUpload } from './upload.models';
import { UploadService } from './upload.service';

@Component({
  selector: 'app-uploads',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './uploads.component.html',
  styleUrl: './uploads.component.scss'
})
export class UploadsComponent implements OnInit {
  readonly bankAccounts = signal<BankAccount[]>([]);
  readonly uploads = signal<StatementUpload[]>([]);
  readonly selectedFile = signal<File | null>(null);
  readonly loading = signal(false);
  readonly uploading = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);
  readonly hasAccounts = computed(() => this.bankAccounts().length > 0);
  readonly dateBasisOptions: Array<{ value: StatementImportDateBasis; label: string; hint: string }> = [
    {
      value: 'TRANSACTION_DATE',
      label: 'Use transaction dates',
      hint: 'Import every parsed row under its actual transaction date.'
    },
    {
      value: 'STATEMENT_MONTH_ONLY',
      label: 'Only statement month',
      hint: 'Ignore rows outside the selected statement month.'
    },
    {
      value: 'CUSTOM_RANGE',
      label: 'Custom date range',
      hint: 'Import only rows within a selected start and end date.'
    }
  ];

  readonly form = this.formBuilder.nonNullable.group({
    bankAccountId: ['', [Validators.required]],
    statementMonth: [this.currentMonth(), [Validators.required]],
    dateBasis: ['TRANSACTION_DATE' as StatementImportDateBasis, [Validators.required]],
    importStartDate: [''],
    importEndDate: [''],
    pdfPassword: ['']
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly bankAccountService: BankAccountService,
    private readonly uploadService: UploadService
  ) {}

  ngOnInit(): void {
    this.loadInitialData();
    this.form.controls.dateBasis.valueChanges.subscribe((dateBasis) => {
      if (dateBasis !== 'CUSTOM_RANGE') {
        this.form.controls.importStartDate.setValue('');
        this.form.controls.importEndDate.setValue('');
      }
    });
  }

  loadInitialData(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.bankAccountService.list().subscribe({
      next: (accounts) => {
        this.bankAccounts.set(accounts);
        if (accounts.length && !this.form.controls.bankAccountId.value) {
          this.form.controls.bankAccountId.setValue(accounts[0].id);
        }
        this.loadUploads(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  loadUploads(showSpinner = true): void {
    if (showSpinner) {
      this.loading.set(true);
    }
    this.errorMessage.set(null);
    this.uploadService.list(this.form.controls.statementMonth.value).subscribe({
      next: (uploads) => {
        this.uploads.set(uploads);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile.set(input.files?.[0] ?? null);
  }

  submit(): void {
    if (this.form.invalid || this.uploading()) {
      this.form.markAllAsTouched();
      return;
    }

    const file = this.selectedFile();
    if (!file) {
      this.errorMessage.set('Select a PDF statement file');
      return;
    }

    const value = this.form.getRawValue();
    if (value.dateBasis === 'CUSTOM_RANGE') {
      if (!value.importStartDate || !value.importEndDate) {
        this.errorMessage.set('Select both custom import dates');
        return;
      }
      if (value.importEndDate < value.importStartDate) {
        this.errorMessage.set('Custom import end date must be on or after the start date');
        return;
      }
    }

    this.uploading.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    this.uploadService.upload({
      bankAccountId: value.bankAccountId,
      statementMonth: value.statementMonth,
      file,
      pdfPassword: this.trimToNull(value.pdfPassword),
      dateBasis: value.dateBasis,
      importStartDate: value.dateBasis === 'CUSTOM_RANGE' ? value.importStartDate : null,
      importEndDate: value.dateBasis === 'CUSTOM_RANGE' ? value.importEndDate : null
    }).subscribe({
      next: (upload) => {
        this.uploading.set(false);
        this.selectedFile.set(null);
        this.successMessage.set(this.uploadSummary(upload));
        this.loadUploads(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.uploading.set(false);
      }
    });
  }

  deleteUpload(upload: StatementUpload): void {
    if (!confirm(`Delete upload ${upload.originalFileName}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.uploadService.delete(upload.id).subscribe({
      next: () => this.loadUploads(),
      error: (error) => this.errorMessage.set(apiErrorMessage(error))
    });
  }

  accountName(bankAccountId: string): string {
    const account = this.bankAccounts().find((item) => item.id === bankAccountId);
    return account ? `${account.accountNickname} - ${account.bankName}` : 'Unknown account';
  }

  statusClass(status: string): string {
    return status.toLowerCase().replace('_', '-');
  }

  customRangeSelected(): boolean {
    return this.form.controls.dateBasis.value === 'CUSTOM_RANGE';
  }

  uploadCountDetail(upload: StatementUpload): string {
    const details = [`${upload.failedRows} failed`];
    if (upload.skippedRows > 0) {
      details.push(`${upload.skippedRows} skipped`);
    }
    if (upload.duplicateRows > 0) {
      details.push(`${upload.duplicateRows} duplicate`);
    }
    return details.join(' | ');
  }

  private currentMonth(): string {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
  }

  private trimToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private uploadSummary(upload: StatementUpload): string {
    const details = [`saved ${upload.parsedRows}`];
    if (upload.skippedRows > 0) {
      details.push(`skipped ${upload.skippedRows}`);
    }
    if (upload.duplicateRows > 0) {
      details.push(`ignored ${upload.duplicateRows} duplicate`);
    }
    return `Parsed ${upload.originalFileName}: ${details.join(', ')}`;
  }
}
