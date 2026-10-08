import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { apiErrorMessage } from '../../shared/api-error';
import { BankAccount, BankAccountRequest } from './bank-account.models';
import { BankAccountService } from './bank-account.service';

@Component({
  selector: 'app-bank-accounts',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './bank-accounts.component.html',
  styleUrl: './bank-accounts.component.scss'
})
export class BankAccountsComponent implements OnInit {
  readonly accounts = signal<BankAccount[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);
  readonly editingAccount = signal<BankAccount | null>(null);
  readonly formTitle = computed(() => this.editingAccount() ? 'Edit bank account' : 'Add bank account');

  readonly form = this.formBuilder.nonNullable.group({
    bankName: ['', [Validators.required, Validators.maxLength(120)]],
    accountNickname: ['', [Validators.required, Validators.maxLength(120)]],
    maskedAccountNumber: ['', [Validators.maxLength(40)]],
    accountType: ['SAVINGS', [Validators.maxLength(40)]]
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly bankAccountService: BankAccountService
  ) {}

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.bankAccountService.list().subscribe({
      next: (accounts) => {
        this.accounts.set(accounts);
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }

    const request = this.toRequest();
    const editingAccount = this.editingAccount();
    const save$ = editingAccount
      ? this.bankAccountService.update(editingAccount.id, request)
      : this.bankAccountService.create(request);

    this.saving.set(true);
    this.errorMessage.set(null);
    save$.subscribe({
      next: () => {
        this.saving.set(false);
        this.resetForm();
        this.loadAccounts();
      },
      error: (error) => {
        this.errorMessage.set(apiErrorMessage(error));
        this.saving.set(false);
      }
    });
  }

  edit(account: BankAccount): void {
    this.editingAccount.set(account);
    this.form.setValue({
      bankName: account.bankName,
      accountNickname: account.accountNickname,
      maskedAccountNumber: account.maskedAccountNumber ?? '',
      accountType: account.accountType || 'SAVINGS'
    });
  }

  deactivate(account: BankAccount): void {
    if (!confirm(`Remove ${account.accountNickname}?`)) {
      return;
    }

    this.errorMessage.set(null);
    this.bankAccountService.delete(account.id).subscribe({
      next: () => this.loadAccounts(),
      error: (error) => this.errorMessage.set(apiErrorMessage(error))
    });
  }

  resetForm(): void {
    this.editingAccount.set(null);
    this.form.reset({
      bankName: '',
      accountNickname: '',
      maskedAccountNumber: '',
      accountType: 'SAVINGS'
    });
  }

  private toRequest(): BankAccountRequest {
    const value = this.form.getRawValue();
    return {
      bankName: value.bankName.trim(),
      accountNickname: value.accountNickname.trim(),
      maskedAccountNumber: this.trimToNull(value.maskedAccountNumber),
      accountType: this.trimToNull(value.accountType)
    };
  }

  private trimToNull(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
